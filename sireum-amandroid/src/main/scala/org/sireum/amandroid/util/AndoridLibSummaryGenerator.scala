package org.sireum.amandroid.util

import org.sireum.amandroid.AndroidGlobalConfig
import org.sireum.jawa.JawaCodeSource
import org.sireum.util.FileUtil
import org.sireum.jawa.GlobalConfig
import org.sireum.jawa.Center
import java.io.File
import java.io.FileOutputStream
import java.util.zip.GZIPOutputStream
import java.io.BufferedOutputStream
import org.sireum.jawa.xml.AndroidXStream
import java.util.zip.GZIPInputStream
import java.io.FileInputStream
import org.sireum.jawa.Center.CenterImage

object AndoridLibSummaryGenerator {
  
  def main(args: Array[String]) {
	  val androidLibDir = AndroidGlobalConfig.android_lib_dir
	  if(androidLibDir != null){
	    val startTime = System.currentTimeMillis()
			JawaCodeSource.preLoad(FileUtil.toUri(androidLibDir), GlobalConfig.PILAR_FILE_EXT)
			var x : Float = 0
			val recSize = JawaCodeSource.getLibraryRecordsCodes.size
			JawaCodeSource.getLibraryRecordsCodes.par.map{
			  case (recName, code) =>
			    this.synchronized(x += 1)
			    if(x%100==0)println((x/recSize)*100 + "%")
			    if(x == recSize) println("Record resolving Done!")
			    Center.resolveRecord(recName, Center.ResolveLevel.HIERARCHY)
			}
	    
	    val outputDir = AndroidGlobalConfig.amandroid_home + "/output"
	  	val fileDir = new File(outputDir + "/libsummary")
	  	if(!fileDir.exists()) fileDir.mkdirs()
	  	val file = new File(fileDir + "/AndroidLibSummary.xml.zip")
	    val w = new FileOutputStream(file)
      val zipw = new GZIPOutputStream(new BufferedOutputStream(w))
	    val img = Center.createImage
	    AndroidXStream.toXml(img, zipw)
	    zipw.close()
	    println("Result stored!")
	    val reader = new GZIPInputStream(new FileInputStream(file))
	    val xmlObject = AndroidXStream.fromXml(reader).asInstanceOf[CenterImage]
      reader.close()
      println("xml loaded!")
      println(img.equals(xmlObject))
			val endTime = System.currentTimeMillis()
			println("Total time: " + (endTime - startTime)/1000 + "s")
	  } else {
	  	System.err.println("Wrong!")
	  }
  }

}