def checkout(gitUrl, keyInComment, prTargetBranch, prCommentBody, credentialsId="", trunkBranch="master") {
    // /run-xxx dep1=release-x.y
    final commentBodyReg = /\b${keyInComment}\s*=\s*([^\s\\]+)(\s|\\|$)/    
    // - release-6.2
    // - release-6.2-20220801
    // - 6.2.0-pitr-dev    
    final releaseOrHotfixBranchReg = /^(release\-)?(\d+\.\d+)(\.\d+\-.+)?/
    // - feature/abcd
    // - feature_abcd
    final featureBranchReg = /^feature[\/_].*/

    def componentBranch = prTargetBranch
    if (prCommentBody =~ commentBodyReg) {
        componentBranch = (prCommentBody =~ commentBodyReg)[0][1]
    } else if (prTargetBranch =~ releaseOrHotfixBranchReg) {
        componentBranch = String.format('release-%s', (prTargetBranch =~ releaseOrHotfixBranchReg)[0][2])
    } else if (prTargetBranch =~ featureBranchReg) {
       componentBranch = trunkBranch
    }

    def pluginSpec = "+refs/heads/*:refs/remotes/origin/*"
    // transfer plugin branch from pr/28 to origin/pr/28/head
    if (componentBranch.startsWith("pr/")) {
        pluginSpec = "+refs/pull/*:refs/remotes/origin/pr/*"
        componentBranch = "origin/${componentBranch}/head"
    }

    checkout(
        changelog: false,
        poll: true,
        scm: [
            $class: 'GitSCM',
            branches: [[name: componentBranch]],
            doGenerateSubmoduleConfigurations: false,
            extensions: [
                [$class: 'PruneStaleBranch'],
                [$class: 'CleanBeforeCheckout'],
                [$class: 'CloneOption', timeout: 2],
            ], 
            submoduleCfg: [],
            userRemoteConfigs: [[
                credentialsId: credentialsId,
                refspec: pluginSpec,
                url: gitUrl,
            ]]
        ]
    )
}