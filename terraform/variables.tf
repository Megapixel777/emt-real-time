variable "docker_image" {
  description = "Docker image used by Cloud Run"
  type        = string
}

variable "emt_client_id_secret" {
  description = "Secret Manager secret containing EMT client ID"
  type        = string
  default     = "emt-client-id"
}

variable "emt_passkey_secret" {
  description = "Secret Manager secret containing EMT passkey"
  type        = string
  default     = "emt-passkey"
}
