import org.apache.spark.sql.{ SparkSession, functions}
import org.apache.spark.sql.types.{StringType, StructField, StructType}


object Application extends App {

  val spark = SparkSession.builder.appName("Ukraine").config("spark.master", "local").getOrCreate()

  val pathCSV = "./ua_reactions.csv"
  val pathTxt = "./excluded.txt"

  var csvDataFrame = spark.read.format("csv").option("header", "true").load(pathCSV) // header = true

  // exclude UAs

  val excludedFileDataFrame = spark.createDataFrame(
    spark.read.text(pathTxt).rdd,
    new StructType().add(StructField("ua", StringType, true)));

  // First task

  csvDataFrame = csvDataFrame
    .join(excludedFileDataFrame, Seq("ua"), "left_anti")

  csvDataFrame.groupBy("ua") // group by ua
    .agg(functions.sum("is_click").alias("clicks"), functions.count("is_click").alias("shows")) //  sum clicks for each ua
    .where("clicks > 5")
    .orderBy(functions.desc("clicks"))
    .withColumn("CTR", functions.col("clicks") / functions.col("shows"))
    .show(5) // show 5

  // Second task

  val total = csvDataFrame.agg(functions.sum("is_click")).first.getDouble(0).toInt
  println("total:" + total)

  val PercentsDF = csvDataFrame.groupBy("ua") // group by ua
    .agg(functions.count("is_click").alias("shows"))
      .withColumn("percents_shows", functions.col("shows") / functions.sum("shows").over() * 100)
      .orderBy(functions.desc("percents_shows"))

}
