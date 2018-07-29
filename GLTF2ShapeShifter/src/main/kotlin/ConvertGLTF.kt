import de.javagl.jgltf.model.GltfModel
import de.javagl.jgltf.model.io.*
import java.io.*
import de.javagl.jgltf.model.*
import de.javagl.jgltf.model.impl.*

fun main(args:Array<String>) {
    val inputFileName = "examples/cube.gltf"
    val gltfModelReader = GltfModelReader()
    val gltfModel = gltfModelReader.read(File(inputFileName).toURI())

    println(gltfModel)
    // Get list of scenes
    println(gltfModel.getSceneModels())
    // Get list of nodes in that scene
    val sceneModels = gltfModel.getSceneModels()
    val scene = sceneModels[0]
    println(scene.getName())



    val nodeModels = scene.getNodeModels()
    println(nodeModels)
    nodeModels.forEach {
        println(it.getName())

        val meshes = it.getMeshModels()
        if (!meshes.isEmpty()) {
            println(meshes)
            meshes.forEach {
                println("Primitive:"+ it.getMeshPrimitiveModels())
                val x = it.getMeshPrimitiveModels()[0]
                println(x.getAttributes())
                println("Indices ="+x.getIndices())
                val indices = x.getIndices()
                println(indices.getElementType())
                println(indices.getCount())
                val buf = indices?.getBufferViewModel()?.getBufferViewData()
                println (buf)
                println("DDD"+ buf?.get(2))

                println("Targets:"+x.getTargets())
                val positions = x.getAttributes()["POSITION"]
                println(positions?.getElementType())
                println(positions?.getCount())
                println(positions?.getBufferViewModel()?.getBufferViewData().getUri())
                println("(${buf?.get(0)}, ${buf?.get(1)}, ${buf?.get(2)}")
            }
        }
    }

    // TODO Look into DefaultMeshPrimitiveModel
    // Perhaps a path per primitive?
}