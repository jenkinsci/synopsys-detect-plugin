package templates.jelly

'j:jelly'('xmlns:j': 'jelly:core', 'xmlns:f': '/lib/form', 'xmlns:c': '/lib/credentials') {
    'f:section'(title: globalConfigSectionTitle) {
        'f:entry'(field: urlField, title: urlTitle) {
            'f:textbox'()
        }
        'f:entry'(field: credentialsField, title: credentialsTitle) {
            'c:select'()
        }
        'f:entry'(field: detectDownloadUrlField, title: detectDownloadUrlTitle) {
            'f:select'(checkmethod: 'post')
        }
        'f:advanced'() {
            'f:entry'(field: detectArtifactUrlField, title: detectArtifactUrlTitle) {
                'f:textbox'()
            }
            'f:entry'(field: timeoutField, title: timeoutTitle) {
                'f:textbox'(clazz: 'required number', checkmethod: 'post')
            }
            'f:entry'(field: trustCertificatesField, title: trustCertificatesTitle) {
                'f:checkbox'(default: 'false')
            }
        }

        'f:validateButton'(method: testConnectionMethod, title: testConnectionTitle, progress: testConnectionProgress, with: "${urlField},${credentialsField},${timeoutField},${trustCertificatesField}")
    }

}
