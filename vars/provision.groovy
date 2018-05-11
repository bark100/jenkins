def call(Closure body) {

    // Ansible inventory state represented as Map object
    def inventory = "moshe"

    println("inventory from outside body" + inventory)

    body()
}
