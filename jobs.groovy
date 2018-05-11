multibranchPipelineJob('jenkins') {
    branchSources {
        branchSource {
            source {
                github {
                    repoOwner("bark100")
                    repository("jenkins")
                    buildOriginBranch(true)
                    includes("master")
                    excludes("")
                }
            }
        }
    }
    orphanedItemStrategy {
        discardOldItems {
            numToKeep(5)
        }
    }
}
