def lib = library("jenkins@master").com.example

Map repoConfig = [
        name: "jenkins",
        rebase: true,
        shallow: true
]

node() {
    // Checkout library branch
    def repo = lib.GitHubRepo.new(this, repoConfig)
    repo.checkoutBranch(branchName)
}
