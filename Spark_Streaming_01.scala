package day09_Streaming




import org.apache.log4j.{Level, Logger}
import org.apache.spark.streaming.dstream.{DStream, InputDStream, ReceiverInputDStream}
import org.apache.spark.SparkConf
import org.apache.spark.streaming.{Seconds, StreamingContext}
/*
* 入口 StreamingContext
*  监听网络某端口实时产生的数据,socket,每2秒提交一次
*  也就是每一次sparkStreaming计算的是每2秒产生的数据
*  注意：
*     如果我们使用local的方式启动streaming程序，必须要使cpu core的个数大于等于2
*     因为我们streaming程序，必须要有一个cpu core分配个receiver取接受外部的数据（优先分配）
*      剩余
* */
object Spark_Streaming_01 {
  def main(args: Array[String]): Unit = {
    if(args==null||args.length<3){
      println(
        """Parameter Error! Usage:<host><port><batchInterval>
          |host         :   连接主机名
          |port         :   连接端口
          |batchInterval:   批次提交间隔时间
        """.stripMargin
      )
    System.exit(-1)
    }
    Logger.getLogger("org.apache.hadoop").setLevel(Level.WARN)
    Logger.getLogger("org.apache.spark").setLevel(Level.WARN)
    Logger.getLogger("org.project-spark").setLevel(Level.WARN)
      val Array(host,port,batchInterval)=args
    val conf=new SparkConf().setMaster("local[2]").setAppName("Spark_Streaming_01")
    val batchDuration=Seconds(batchInterval.toLong)
//  入口
   val scc=new StreamingContext(conf,batchDuration)
    //加载外部网络端口数据
          val linesDStream: ReceiverInputDStream[String] = scc.socketTextStream(host,port.toInt)
//    linesDStream.print()
    val rbkDStream: DStream[(String, Int)] = linesDStream.flatMap(line=>line.split(" ")).map(word=>(word,1)).reduceByKey(_+_)
    rbkDStream.print()

//要想让sparkStreaming程序启动，必须要调用start()方法
    println("----------------start 之前-----------------------")
    scc.start()
    println("----------------start 之后-----------------------")
    scc.awaitTermination()

  }
}
