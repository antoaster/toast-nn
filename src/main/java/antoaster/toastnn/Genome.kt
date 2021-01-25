package antoaster.toastnn

data class Genome (
        val nodes: List<NodeGene>,
        val connections: List<ConnectionGene>
) {
    val inputs: Int
    val outputs: Int

    init {
        inputs = nodes.count { it.nodeType == NodeType.INPUT }
        outputs = nodes.count { it.nodeType == NodeType.OUTPUT }
        // TODO: validate ordering
    }

    fun generateNetwork(): Network {
        val networkNodes = nodes.mapIndexed { index, nodeGene ->
            require(nodeGene.index == index) { "Something went wrong - expected index $index on ${nodeGene}"}
            Node(nodeGene) }
        val networkConnections = connections.map { gene ->
            // TODO: validate these return something
            val from = networkNodes[gene.fromIndex]
            val to = networkNodes[gene.toIndex]
            Connection(gene, from, to, gene.weight, gene.enabled)
        }

        return Network(this, networkNodes, networkConnections)
    }
}

enum class NodeType {
    INPUT,
    OUTPUT,
    HIDDEN
}

data class NodeGene (
        val index: Int,
        val nodeType: NodeType
)

data class ConnectionGene (
        val fromIndex: Int,
        val toIndex: Int,
        val innovation: Int,
        val weight: Float,
        val enabled: Boolean
)



data class Network(
        val genome: Genome,
        val nodes: List<Node>,
        val connections: List<Connection>
) {
    fun maxDepth(nodeIndex: Int): Int {
        val node = nodes[nodeIndex]

        if (node.gene.nodeType == NodeType.INPUT) return 0
//         TODO: detect cycles and such
        val parents = connections.filter { it.toNode == node }.map { it.fromNode.gene.index }
        return parents.map { maxDepth(it) + 1}.maxOrNull() ?: throw RuntimeException("Kaboom?")
    }
}

data class Node (
    val gene: NodeGene
)

data class Connection (
        val gene: ConnectionGene,
        val fromNode: Node,
        val toNode: Node,
        val weight: Float,
        val enabled: Boolean
)