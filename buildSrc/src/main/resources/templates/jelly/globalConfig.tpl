package templates.jelly

'j:jelly'('xmlns:j': 'jelly:core', 'xmlns:f': '/lib/form', 'xmlns:c': '/lib/credentials') {
    'f:section'(title: globalConfigSectionTitle) {
        'f:entry'(field: blackDuckUrlField, title: blackDuckUrlTitle) {
            'f:textbox'()
        }

        'f:entry'(field: blackDuckCredentialsField, title: blackDuckCredentialsTitle) {
            'c:select'()
        }

        'f:advanced'() {
            'f:entry'(field: blackDuckTimeoutField, title: blackDuckTimeoutTitle) {
                'f:textbox'(clazz: 'required number', checkmethod: 'post')
            }
            'f:entry'(field: trustBlackDuckCertificatesField, title: trustBlackDuckCertificatesTitle) {
                'f:checkbox'(default: 'false')
            }
        }

        'f:validateButton'(method: testBlackDuckConnectionMethod, title: testBlackDuckConnectionTitle, progress: testConnectionProgress, with: "${blackDuckUrlField},${blackDuckCredentialsField},${blackDuckTimeoutField},${trustBlackDuckCertificatesField}")


        'f:entry'(field: polarisUrlField, title: polarisUrlTitle) {
            'f:textbox'()
        }

        'f:entry'(field: polarisCredentialsField, title: polarisCredentialsTitle) {
            'c:select'()
        }

        'f:advanced'() {
            'f:entry'(field: polarisTimeoutField, title: polarisTimeoutTitle) {
                'f:textbox'(clazz: 'required number', checkmethod: 'post')
            }
            'f:entry'(field: trustPolarisCertificatesField, title: trustPolarisCertificatesTitle) {
                'f:checkbox'(default: 'false')
            }
        }

        'f:validateButton'(method: testPolarisConnectionMethod, title: testPolarisConnectionTitle, progress: testConnectionProgresss, with: "${polarisUrlField},${polarisCredentialsTitle},${polarisTimeoutField},${trustPolarisCertificatesField}")

    }

}
