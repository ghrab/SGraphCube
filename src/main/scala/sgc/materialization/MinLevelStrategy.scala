package sgc.materialization

import sgc.cuboid.{CuboidQuery, Cuboid, CuboidEntry}
import spark.storage.StorageLevel
import spark.Logging


object MinLevelStrategy extends Logging {

  def materialize(maxCuboids : Int, minLevel : Int, numberOfDimensions : Int, baseCuboid : CuboidEntry) : GraphCube = {

    val graphCube = new GraphCube(numberOfDimensions,minLevel,baseCuboid)

    def generateCuboid(count : Int, aggregateLevel : Int, combinations : Seq[Seq[Int]]) : Unit = {

      if (count == maxCuboids) return
      combinations match{
        case Nil if aggregateLevel != 0 =>  {
          generateCuboid(count, aggregateLevel - 1, CombinationsGenerator.comb(aggregateLevel,numberOfDimensions))
        }
        case head :: tail => {
          val startCuboidMat = System.currentTimeMillis()
          val fun = new Cuboid(head)
          logInfo("Materializing cuboid " + fun + " from base cuboid")
          val cuboid = CuboidQuery.cuboidQuery(baseCuboid.cuboid, fun)
          cuboid.persist(StorageLevel.DISK_ONLY)

          val size = cuboid.count()   //triggers the materialization of the cuboid
          println("Cuboid " + fun + " materialized in " + (System.currentTimeMillis() - startCuboidMat))
          logInfo("Cuboid added to the graphcube, with size : " + size)
          graphCube.addCuboid(CuboidEntry(fun, size,cuboid))
          generateCuboid(count + 1, aggregateLevel, tail)
        }
        case _ => {
          logInfo("Base cuboid reached")
          return
        }
      }
    }

    generateCuboid(0,numberOfDimensions - minLevel, Nil)
    logInfo("Materialization finished")
    graphCube


  }

}
