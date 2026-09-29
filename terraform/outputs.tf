output "cloud_run_url" {
  description = "Public URL of the EMT Real-Time API"
  value       = google_cloud_run_v2_service.emt_api.uri
}
