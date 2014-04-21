package org.sireum.amandroid.test.framework.security

import org.sireum.jawa.test.framework.TestFramework
import org.sireum.util._
import org.sireum.amandroid._
import org.sireum.jawa.util.APKFileResolver
import java.io._
import org.sireum.amandroid.alir.interProcedural.reachingFactsAnalysis._
import org.sireum.amandroid.alir.interProcedural.taintAnalysis._
import org.sireum.jawa.alir.interProcedural.dataDependenceAnalysis.InterproceduralDataDependenceAnalysis
import java.net.URI
import org.sireum.amandroid.alir.AppCenter
import org.sireum.amandroid.alir.dataRecorder.DataCollector
import org.sireum.amandroid.alir.dataRecorder.MetricRepo
import org.sireum.amandroid.alir.AndroidGlobalConfig
import org.sireum.jawa.MessageCenter._
import org.sireum.amandroid.alir.AndroidConstants
import org.sireum.jawa.JawaCodeSource
import org.sireum.jawa.Center
import org.sireum.jawa.ClassLoadManager
import org.sireum.amandroid.android.decompile.Dex2PilarConverter
import org.sireum.amandroid.android.util.AndroidLibraryAPISummary
import org.sireum.jawa.util.IgnoreException
import org.sireum.jawa.util.TimeOutException
import org.sireum.jawa.util.Timer
import org.sireum.jawa.GlobalConfig
import org.sireum.amandroid.android.appInfo.AppInfoCollector
import org.sireum.amandroid.security.oauth.OAuthSourceAndSinkManager
import org.sireum.amandroid.security.oauth.OauthTokenContainerCollector

object OAuthTokenCounter {
  var total = 0
  var oversize = 0
  var haveresult = 0
  var haveOauthToken = 0
  var foundOauthContainer = 0
  var taintPathFound = 0
  var haveOauthTokenStatementList = Set[String]()
  var foundOauthContainerList = Set[String]()
  var taintPathFoundList = Set[String]()
  override def toString : String = "total: " + total + ", oversize: " + oversize + ", haveResult: " + haveresult + ", haveOauthToken: " + haveOauthToken + ", foundOauthContainer: " + foundOauthContainer + ", taintPathFound: " + taintPathFound
  
  val appRec = mmapEmpty[String, Int]
  def addRecs(names : Iterable[String]) = {
    names.foreach{
      n =>
        if(appRec.contains(n)){
          appRec(n) = appRec(n) + 1
        }
        else appRec(n) = 1
    }
  }
  
  def outputRecStatistic = {
  	val outputDir = System.getenv(AndroidGlobalConfig.ANDROID_OUTPUT_DIR)
  	if(outputDir == null) throw new RuntimeException("Does not have env var: " + AndroidGlobalConfig.ANDROID_OUTPUT_DIR)
  	val appDataDirFile = new File(outputDir + "/recStatistic")
  	if(!appDataDirFile.exists()) appDataDirFile.mkdirs()
  	val out = new PrintWriter(appDataDirFile + "/RecStatistic.txt")
    appRec.filter(p=> p._2 >= 5).toSeq.sortBy(_._1).sortBy(_._2).foreach(out.println(_))
    out.close()
  }
  
  def outputInterestingFileNames = {
  	val outputDir = System.getenv(AndroidGlobalConfig.ANDROID_OUTPUT_DIR)
  	if(outputDir == null) throw new RuntimeException("Does not have env var: " + AndroidGlobalConfig.ANDROID_OUTPUT_DIR)
  	val appDataDirFile = new File(outputDir + "/interestingApps")
  	if(!appDataDirFile.exists()) appDataDirFile.mkdirs()
  	val out = new PrintWriter(appDataDirFile + "/interestingApps.txt")
    out.println("HavePasswordViewList:")
    haveOauthTokenStatementList.foreach(out.println(_))
    out.println("\n\n\n\nfoundPasswordContainerList:")
    foundOauthContainerList.foreach(out.println(_))
    out.println("\n\n\n\ntaintPathFoundList:")
    taintPathFoundList.foreach(out.println(_))
    out.close()
  }
}

trait OAuthTokenTrackingTestFramework extends TestFramework {
  private final val TITLE = "OAuthTokenTrackingTestFramework"
  def Analyzing : this.type = this

  def title(s : String) : this.type = {
    _title = caseString + s
    this
  }

  def file(fileRes : FileResourceUri) =
    InterProceduralConfiguration(title, fileRes)
/**
 * does inter procedural analysis of an app
 * @param src is the uri of the apk file
 */
  case class InterProceduralConfiguration //
  (title : String,
   srcRes : FileResourceUri) {

    test(title) {
    	msg_critical(TITLE, "####" + title + "#####")
    	OAuthTokenCounter.total += 1
    	// before starting the analysis of the current app, first init
    	AndroidGlobalConfig.initJawaAlirInfoProvider
    	
    	val srcFile = new File(new URI(srcRes))
    	val dexFile = APKFileResolver.getDexFile(srcRes, FileUtil.toUri(srcFile.getParentFile()))
    	
    	// convert the dex file to the "pilar" form
    	val pilarFileUri = Dex2PilarConverter.convert(dexFile)
    	val pilarFile = new File(new URI(pilarFileUri))
    	if(pilarFile.length() <= (100 * 1024 * 1024)){
    		AndroidRFAConfig.setupCenter
	    	//store the app's pilar code in AmandroidCodeSource which is organized record by record.
	    	JawaCodeSource.load(pilarFileUri, GlobalConfig.PILAR_FILE_EXT, AndroidLibraryAPISummary)
	    	OAuthTokenCounter.addRecs(JawaCodeSource.getAppRecordsCodes.keys)
	    	try{
	    	  
	    	  if(
	    	    !(JawaCodeSource.getAppRecordsCodes.exists{
	    	      case (sig, code) =>
	    	        code.contains("access_token")
	    	  })
	    	  ) throw new IgnoreException
	    	  
		    	val pre = new OauthTokenContainerCollector(srcRes)
				  pre.collectInfo
				  
				  val ssm = new OAuthSourceAndSinkManager(pre.getPackageName, pre.getLayoutControls, pre.getCallbackMethods, AndroidGlobalConfig.SourceAndSinkFilePath)
		    	var entryPoints = Center.getEntryPoints(AndroidConstants.MAINCOMP_ENV)
		    	entryPoints ++= Center.getEntryPoints(AndroidConstants.COMP_ENV)
		    	val iacs = pre.getInterestingContainers(Set("access_token"))
		    	entryPoints = entryPoints.filter(e=>iacs.contains(e.getDeclaringRecord))
		    	if(!entryPoints.isEmpty){
		    	  OAuthTokenCounter.foundOauthContainer += 1
		    	  OAuthTokenCounter.foundOauthContainerList += title
		    	}
				
	    	  AndroidReachingFactsAnalysisConfig.k_context = 1
			    AndroidReachingFactsAnalysisConfig.resolve_icc = true
			    AndroidReachingFactsAnalysisConfig.resolve_static_init = false
			    AndroidReachingFactsAnalysisConfig.timerOpt = Some(new Timer(10))
		    	entryPoints.par.foreach{
		    	  ep =>
		    	    try{
			    	    msg_critical(TITLE, "--------------Component " + ep + "--------------")
			    	    val initialfacts = AndroidRFAConfig.getInitialFactsForMainEnvironment(ep)
			    	    val (icfg, irfaResult) = AndroidReachingFactsAnalysis(ep, initialfacts, new ClassLoadManager)
			    	    AppCenter.addInterproceduralReachingFactsAnalysisResult(ep.getDeclaringRecord, icfg, irfaResult)
			    	    msg_critical(TITLE, "processed-->" + icfg.getProcessed.size)
			    	    val iddResult = InterproceduralDataDependenceAnalysis(icfg, irfaResult)
//			    	    iddResult.getIddg.toDot(new PrintWriter(System.out))
			    	    AppCenter.addInterproceduralDataDependenceAnalysisResult(ep.getDeclaringRecord, iddResult)
			    	    val tar = AndroidDataDependentTaintAnalysis(iddResult, irfaResult, ssm)    
			    	    AppCenter.addTaintAnalysisResult(ep.getDeclaringRecord, tar)
				    	} catch {
		    	      case te : TimeOutException => System.err.println("Timeout!")
		    	    }
    	    } 
				  
		    	if(AppCenter.getTaintAnalysisResults.exists(!_._2.getTaintedPaths.isEmpty)){
    	      OAuthTokenCounter.taintPathFound += 1
    	      OAuthTokenCounter.taintPathFoundList += title
    	    }
		    	val appData = DataCollector.collect
		    	MetricRepo.collect(appData)
		    	val outputDir = System.getenv(AndroidGlobalConfig.ANDROID_OUTPUT_DIR)
		    	if(outputDir == null) throw new RuntimeException("Does not have env var: " + AndroidGlobalConfig.ANDROID_OUTPUT_DIR)
		    	val apkName = title.substring(0, title.lastIndexOf("."))
		    	val appDataDirFile = new File(outputDir + "/" + apkName)
		    	if(!appDataDirFile.exists()) appDataDirFile.mkdirs()
		    	val out = new PrintWriter(appDataDirFile + "/AppData.txt")
			    out.print(appData.toString)
			    out.close()
			    val mr = new PrintWriter(outputDir + "/MetricInfo.txt")
				  mr.print(MetricRepo.toString)
				  mr.close()
				  OAuthTokenCounter.haveresult += 1
	    	} catch {
	    	  case ie : IgnoreException =>
	    	    err_msg_critical(TITLE, "Ignored!")
	    	  case re : RuntimeException => 
	    	    re.printStackTrace()
	    	  case e : Exception =>
	    	    e.printStackTrace()
	    	} finally {
	    	}
	    	
	//    	val r = Center.resolveRecord("[|java:lang:Class|]", Center.ResolveLevel.BODIES)
	//    	r.getProcedures.toSeq.sortBy(f => f.getSignature).foreach{
	//    	  p =>
	//    	    println("  case \"" + p.getSignature + "\" =>  //" + p.getAccessFlagString)
	//    	}
    	} else {
    	  OAuthTokenCounter.oversize += 1
    	  err_msg_critical(TITLE, "Pilar file size is too large:" + pilarFile.length()/1024/1024 + "MB")
    	}
    	
    	Center.reset
    	AppCenter.reset
    	// before starting the analysis of the current app, first clear the previous app's records' code from the AmandroidCodeSource
    	JawaCodeSource.clearAppRecordsCodes
    	System.gc()
    	System.gc()
    	msg_critical(TITLE, OAuthTokenCounter.toString)
//    	OAuthTokenCounter.outputInterestingFileNames
//    	OAuthTokenCounter.outputRecStatistic
    	msg_critical(TITLE, "************************************\n")
    }
  }

  protected var _title : String = null
  protected var num = 0
  protected def title() = if (_title == null) {
    num += 1
    "Analysis #" + num
  } else _title
}