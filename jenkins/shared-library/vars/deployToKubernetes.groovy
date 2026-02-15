/*
 * Shared Library: Deploy to Kubernetes
 *
 * Reusable function for deploying applications to Kubernetes
 * Supports both Kustomize and Helm deployment tools
 *
 * Usage:
 * @Library('jenkins-shared-library') _
 * deployToKubernetes(
 *     environment: 'production',
 *     imageName: 'springboot-app',
 *     imageTag: 'v1.0.0',
 *     deploymentTool: 'helm'
 * )
 */

def call(Map config) {
    def environment = config.environment ?: 'dev'
    def imageName = config.imageName ?: 'springboot-app'
    def imageTag = config.imageTag ?: 'latest'
    def deploymentTool = config.deploymentTool ?: 'kustomize'
    def namespace = config.namespace ?: environment
    def timeout = config.timeout ?: '5m'
    def dryRun = config.dryRun ?: false

    echo "====== Deploying to Kubernetes ======"
    echo "Environment: ${environment}"
    echo "Image: ${imageName}:${imageTag}"
    echo "Tool: ${deploymentTool}"
    echo "Namespace: ${namespace}"

    try {
        // Install required tools
        sh '''
            apk add --no-cache kubectl helm kustomize curl git
            kubectl version --client
        '''

        // Create namespace if it doesn't exist
        sh '''
            kubectl create namespace ${namespace} --dry-run=client -o yaml | kubectl apply -f -
        '''

        if (deploymentTool == 'helm') {
            deployWithHelm(imageName, imageTag, environment, namespace, timeout, dryRun)
        } else {
            deployWithKustomize(imageName, imageTag, environment, namespace, timeout, dryRun)
        }

        echo "✓ Deployment completed successfully"
    } catch (Exception e) {
        echo "✗ Deployment failed: ${e.message}"
        throw e
    }
}

private def deployWithHelm(String imageName, String imageTag, String environment, String namespace, String timeout, Boolean dryRun) {
    echo "Deploying with Helm..."

    def helmArgs = "--namespace ${namespace} " +
        "--set image.repository=${imageName} " +
        "--set image.tag=${imageTag} " +
        "--set global.environment=${environment} " +
        "-f kubernetes/helm/springboot-app/values-${environment}.yaml "

    if (dryRun) {
        helmArgs += "--dry-run --debug"
    } else {
        helmArgs += "--wait --timeout ${timeout}"
    }

    sh '''
        helm upgrade --install ${imageName} \
            kubernetes/helm/springboot-app \
            ${helmArgs}
    '''

    if (!dryRun) {
        sh '''
            kubectl rollout status deployment/${imageName} \
                -n ${namespace} \
                --timeout=${timeout}
        '''
    }
}

private def deployWithKustomize(String imageName, String imageTag, String environment, String namespace, String timeout, Boolean dryRun) {
    echo "Deploying with Kustomize..."

    sh '''
        cd kubernetes/kustomize/overlays/${environment}
        kustomize edit set image ${imageName}=${imageName}:${imageTag}
        cd ${WORKSPACE}
    '''

    def dryRunFlag = dryRun ? "--dry-run=client" : ""

    sh '''
        kustomize build kubernetes/kustomize/overlays/${environment} | \
            kubectl apply -f - \
            -n ${namespace} \
            ${dryRunFlag}
    '''

    if (!dryRun) {
        sh '''
            kubectl rollout status deployment/${imageName} \
                -n ${namespace} \
                --timeout=${timeout}
        '''
    }
}
