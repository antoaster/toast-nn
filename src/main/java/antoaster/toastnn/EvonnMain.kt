package antoaster.toastnn

import kotlin.random.Random


object Main {
    @JvmStatic
    fun main(args: Array<String>) {

        val context = createContext()

        println("${context.network}")

        val display = EvonnDisplay()
        display.initialize(initialContext = context)

    }

    private var innovation = 0

    private fun createContext(): EvonnContext {
        val genome: Genome = createGenome(
                inputs = 2,
                outputs = 2,
                hiddenLayers = 1,
                nodesPerLayer = 3
        )

        return EvonnContext(genome.generateNetwork())
    }

    private fun createGenome(inputs: Int, outputs: Int, hiddenLayers: Int, nodesPerLayer: Int): Genome {

        val total = inputs + outputs + (hiddenLayers * nodesPerLayer)

        val nodes = (0 until total).map { i ->
            NodeGene(i, when {
                i < inputs -> NodeType.INPUT
                i < inputs + outputs -> NodeType.OUTPUT
                else -> NodeType.HIDDEN
            })
        }

        val hiddenLayerIndexes =
                (0 until hiddenLayers).map { layer ->
                    val base = inputs + outputs
                    val offset = layer * nodesPerLayer
                    val fromClosed = base + offset
                    val toOpen = fromClosed + nodesPerLayer
                    fromClosed until toOpen
                }
        val inputIndexes = 0 until inputs
        val outputIndexes = inputs until inputs + outputs


        val layerIndexes = mutableListOf<IntRange>()
        layerIndexes.add(inputIndexes)
        layerIndexes.addAll(hiddenLayerIndexes)
        layerIndexes.add(outputIndexes)

        val connections = mutableListOf<ConnectionGene>()

        (0 until layerIndexes.size - 1).forEach { i ->
            val fromLayerIndexes = layerIndexes[i]
            val toLayerIndexes = layerIndexes[i+1]
            connections.addAll(fullyConnectLayers(fromLayerIndexes, toLayerIndexes))
        }

        return Genome(nodes, connections)
    }

    private fun fullyConnectLayers(fromLayerIndexes: IntRange, toLayerIndexes: IntRange): Collection<ConnectionGene> {
        return fromLayerIndexes.map { fromIndex ->
            toLayerIndexes.map { toIndex ->
                ConnectionGene(fromIndex, toIndex, innovation++, Random.nextFloat(), Random.nextBoolean())
            }
        }.flatten()
    }
}