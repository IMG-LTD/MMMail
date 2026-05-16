{{- define "mmmail.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{- define "mmmail.fullname" -}}
{{- if .Values.fullnameOverride -}}
{{- .Values.fullnameOverride | trunc 63 | trimSuffix "-" -}}
{{- else -}}
{{- $name := include "mmmail.name" . -}}
{{- if contains $name .Release.Name -}}
{{- .Release.Name | trunc 63 | trimSuffix "-" -}}
{{- else -}}
{{- printf "%s-%s" .Release.Name $name | trunc 63 | trimSuffix "-" -}}
{{- end -}}
{{- end -}}
{{- end -}}

{{- define "mmmail.backendName" -}}
{{- printf "%s-backend" (include "mmmail.fullname" .) | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{- define "mmmail.frontendName" -}}
{{- printf "%s-frontend-admin" (include "mmmail.fullname" .) | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{- define "mmmail.secretName" -}}
{{- default (printf "%s-runtime" (include "mmmail.fullname" .)) .Values.secrets.existingSecret | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{- define "mmmail.labels" -}}
helm.sh/chart: {{ printf "%s-%s" .Chart.Name .Chart.Version | quote }}
app.kubernetes.io/name: {{ include "mmmail.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end -}}

{{- define "mmmail.backendSelectorLabels" -}}
app.kubernetes.io/name: {{ include "mmmail.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
app.kubernetes.io/component: backend
{{- end -}}

{{- define "mmmail.frontendSelectorLabels" -}}
app.kubernetes.io/name: {{ include "mmmail.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
app.kubernetes.io/component: frontend-admin
{{- end -}}
