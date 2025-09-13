# Vault Agent Configuration for Gasolinera JSM
# Handles automatic authentication and secret retrieval

# Exit after authentication and template rendering
exit_after_auth = false

# PID file for the agent
pid_file = "/vault/agent/vault-agent.pid"

# Auto-auth configuration
auto_auth {
  method "approle" {
    mount_path = "auth/approle"
    config = {
      role_id_file_path = "/vault/config/role-id"
      secret_id_file_path = "/vault/config/secret-id"
      remove_secret_id_file_after_reading = false
    }
  }

  sink "file" {
    config = {
      path = "/vault/agent/token"
      mode = 0640
    }
  }
}

# Vault server configuration
vault {
  address = "http://vault:8200"
  retry {
    num_retries = 5
  }
}

# Cache configuration
cache {
  use_auto_auth_token = true
}

# API proxy listener
listener "tcp" {
  address = "0.0.0.0:8100"
  tls_disable = true
}

# Template for application.yml with secrets
template {
  source      = "/vault/templates/application.yml.tpl"
  destination = "/vault/secrets/application.yml"
  perms       = 0640
  command     = "echo 'Application configuration updated'"
}

# Template for database configuration
template {
  source      = "/vault/templates/database.yml.tpl"
  destination = "/vault/secrets/database.yml"
  perms       = 0640
}

# Template for JWT configuration
template {
  source      = "/vault/templates/jwt.properties.tpl"
  destination = "/vault/secrets/jwt.properties"
  perms       = 0640
}

# Template for Redis configuration
template {
  source      = "/vault/templates/redis.yml.tpl"
  destination = "/vault/secrets/redis.yml"
  perms       = 0640
}

# Template for RabbitMQ configuration
template {
  source      = "/vault/templates/rabbitmq.yml.tpl"
  destination = "/vault/secrets/rabbitmq.yml"
  perms       = 0640
}

# Template for external API keys
template {
  source      = "/vault/templates/external-apis.properties.tpl"
  destination = "/vault/secrets/external-apis.properties"
  perms       = 0640
}