resource "google_artifact_registry_repository" "emt_api" {
  location      = "europe-southwest1"
  repository_id = "emt-real-time"
  description   = "Docker images for EMT Real-Time API"
  format        = "DOCKER"
}
