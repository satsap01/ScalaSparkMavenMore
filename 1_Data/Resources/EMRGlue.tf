############################################################
# ✅ Provider Configuration
############################################################

provider "aws" {
  region = "us-east-2"
}

resource "aws_key_pair" "deployer" {
  key_name   = var.key_name
  public_key = file(var.public_key_path)
}

############################################################
# ✅ Networking: VPC, Subnet, Internet Access
############################################################

# Create a custom VPC
resource "aws_vpc" "emr_vpc" {
  cidr_block = "10.0.0.0/16"
}

# Create an internet gateway for internet access
resource "aws_internet_gateway" "emr_igw" {
  vpc_id = aws_vpc.emr_vpc.id
}

# Create a public subnet
resource "aws_subnet" "emr_subnet" {
  vpc_id                  = aws_vpc.emr_vpc.id
  cidr_block              = "10.0.1.0/24"
  map_public_ip_on_launch = true
}

# Create a route table with internet access
resource "aws_route_table" "emr_rt" {
  vpc_id = aws_vpc.emr_vpc.id

  route {
    cidr_block = "0.0.0.0/0"
    gateway_id = aws_internet_gateway.emr_igw.id
  }
}

# Associate subnet with route table
resource "aws_route_table_association" "emr_rta" {
  subnet_id      = aws_subnet.emr_subnet.id
  route_table_id = aws_route_table.emr_rt.id
}

############################################################
# ✅ Security Group: Allow all traffic (simplified for demo)
############################################################

resource "aws_security_group" "emr_sg" {
  name        = "emr-sg"
  description = "Allow all inbound/outbound (demo)"
  vpc_id      = aws_vpc.emr_vpc.id

  ingress {
    description = "SSH from anywhere"
    from_port   = 22
    to_port     = 22
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }
  ingress {
    from_port   = 8088
    to_port     = 8088
    protocol    = "tcp"
    cidr_blocks = ["78.146.211.50/32"] # your public IP with /32 for single IP
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}

############################################################
# ✅ IAM: EMR Service Role
############################################################

resource "aws_iam_role" "emr_service_role" {
  name = "EMR_DefaultRole"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect    = "Allow"
      Principal = { Service = "elasticmapreduce.amazonaws.com" }
      Action    = "sts:AssumeRole"
    }]
  })
}

resource "aws_iam_role_policy_attachment" "emr_service_policy" {
  role       = aws_iam_role.emr_service_role.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonElasticMapReduceRole"
}

############################################################
# ✅ IAM: EC2 Instance Role for EMR nodes
############################################################

resource "aws_iam_role" "emr_ec2_role" {
  name = "EMR_EC2_DefaultRole"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect    = "Allow"
      Principal = { Service = "ec2.amazonaws.com" }
      Action    = "sts:AssumeRole"
    }]
  })
}

# Attach required policies to EC2 role
resource "aws_iam_role_policy_attachment" "emr_ec2_emr_policy" {
  role       = aws_iam_role.emr_ec2_role.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonElasticMapReduceforEC2Role"
}

resource "aws_iam_role_policy_attachment" "emr_ec2_glue_policy" {
  role       = aws_iam_role.emr_ec2_role.name
  policy_arn = "arn:aws:iam::aws:policy/AWSGlueConsoleFullAccess"
}

resource "aws_iam_role_policy_attachment" "emr_ec2_s3_policy" {
  role       = aws_iam_role.emr_ec2_role.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonS3FullAccess"
}

############################################################
# **// ADDED: IAM policy for AWS CodeArtifact to allow publishing packages
############################################################

resource "aws_iam_policy" "codeartifact_publish_policy" {
  name        = "CodeArtifactPublishPolicy"
  description = "Policy to allow pushing packages to AWS CodeArtifact"

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = [
          "codeartifact:PublishPackageVersion",
          "codeartifact:PutPackageMetadata"
        ]
        Resource = "*" # Optionally, restrict to your CodeArtifact repo ARN
      }
    ]
  })
}

# Attach the CodeArtifact publish policy to the EC2 role so that your GitHub actions or EC2 can upload artifacts
resource "aws_iam_role_policy_attachment" "emr_ec2_codeartifact_policy" {
  role       = aws_iam_role.emr_ec2_role.name
  policy_arn = aws_iam_policy.codeartifact_publish_policy.arn
}

############################################################
# **// ADDED: Reference existing S3 bucket sat-ailmt-code for build artifact uploads
############################################################

data "aws_s3_bucket" "artifact_bucket" {
  bucket = "sat-ailmt-code"  # Existing bucket, no creation here
}

############################################################
# Create EC2 instance profile for EMR nodes
############################################################

resource "aws_iam_instance_profile" "emr_instance_profile" {
  name = "EMR_EC2_InstanceProfile"
  role = aws_iam_role.emr_ec2_role.name
}

############################################################
# ✅ EMR Cluster: Single-Node, Spot, Glue, Spark
############################################################

resource "aws_emr_cluster" "single_node_emr" {
  depends_on = [
    aws_iam_instance_profile.emr_instance_profile,
    aws_key_pair.deployer,
    aws_iam_role.emr_service_role
  ]
  name          = "emr-glue-spark"
  release_label = "emr-6.15.0" # Spark 3.4.1

  applications = ["Spark", "Hive"]

  service_role = aws_iam_role.emr_service_role.name
  #  job_flow_role = aws_iam_instance_profile.emr_instance_profile.name
  log_uri = "s3://sat-ailmt-output/logs/" # ← replace this

  # Auto termination after 1 hour idle
  auto_termination_policy {
    idle_timeout = 3600
  }

  keep_job_flow_alive_when_no_steps = true
  visible_to_all_users              = true
  termination_protection            = false

  ec2_attributes {
    subnet_id                         = aws_subnet.emr_subnet.id
    emr_managed_master_security_group = aws_security_group.emr_sg.id
    emr_managed_slave_security_group  = aws_security_group.emr_sg.id
    instance_profile                  = aws_iam_instance_profile.emr_instance_profile.name
    key_name                          = aws_key_pair.deployer.key_name # ✅ This line
  }

  # Use Spot instances for Master node
  master_instance_fleet {
    target_on_demand_capacity = 0
    target_spot_capacity      = 1

    instance_type_configs {
      instance_type = "m5.xlarge"
    }

    launch_specifications {
      spot_specification {
        allocation_strategy      = "capacity-optimized"
        timeout_duration_minutes = 10
        timeout_action           = "TERMINATE_CLUSTER"
        block_duration_minutes   = 0
      }
    }
  }

  # No core or task nodes
  #core_instance_fleet {
  #  target_on_demand_capacity = 0
  #  target_spot_capacity      = 0
  #}

  # Enable Glue Catalog integration
  configurations_json = <<EOF
[
  {
    "Classification": "hive-site",
    "Properties": {
      "hive.metastore.client.factory.class": "com.amazonaws.glue.catalog.metastore.AWSGlueDataCatalogHiveClientFactory"
    }
  }
]
EOF

  tags = {
    Name        = "SingleNodeEMR"
    Environment = "dev"
  }
}

#######################
# Glue Catalog Objects #
#######################

resource "aws_glue_catalog_database" "userdb" {
  name = "userdb"
}

resource "aws_glue_catalog_table" "employee" {
  name          = "employee"
  database_name = aws_glue_catalog_database.userdb.name
  table_type    = "EXTERNAL_TABLE"

  parameters = {
    "classification" = "csv"
  }

  storage_descriptor {
    location      = "s3://sat-ailmt-business/employee/"
    input_format  = "org.apache.hadoop.mapred.TextInputFormat"
    output_format = "org.apache.hadoop.hive.ql.io.HiveIgnoreKeyTextOutputFormat"

    ser_de_info {
      serialization_library = "org.apache.hadoop.hive.serde2.lazy.LazySimpleSerDe"
      parameters = {
        "field.delim"          = ","
        "serialization.format" = ","
      }
    }
    columns {
      name = "eid"
      type = "int"
    }

    columns {
      name = "name"
      type = "string"
    }

  }
}

variable "key_name" {
  description = "SSH key pair name"
  default     = "terra-key-new-rsa"
}

variable "public_key_path" {
  description = "Path to your public key"
  default     = "../terra-key-new-rsa.pub"
}

data "aws_instance" "master" {
  filter {
    name   = "tag:aws:elasticmapreduce:instance-group-role"
    values = ["MASTER"]
  }

  filter {
    name   = "tag:aws:elasticmapreduce:job-flow-id"
    values = [aws_emr_cluster.single_node_emr.id]
  }

  depends_on = [aws_emr_cluster.single_node_emr]
}

output "emr_master_public_ip" {
  value = data.aws_instance.master.public_ip
}

output "emr_master_public_dns" {
  value = data.aws_instance.master.public_dns
}

############################################################
# **// ADDED: Output S3 bucket name for reference in CI/CD pipelines etc.
############################################################

output "artifact_bucket_name" {
  value       = data.aws_s3_bucket.artifact_bucket.bucket
  description = "Existing S3 bucket name used for storing build artifacts"
}
