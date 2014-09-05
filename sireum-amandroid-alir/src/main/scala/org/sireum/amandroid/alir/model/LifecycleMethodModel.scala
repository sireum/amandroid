package org.sireum.amandroid.alir.model

import org.sireum.jawa.JawaProcedure
import org.sireum.util._
import org.sireum.jawa.alir.Context
import org.sireum.jawa.alir.reachingFactsAnalysis._

object LifecycleMethodModel {
	def isLifecycleMethod(p : JawaProcedure) : Boolean = {
	  p.getSignature match{
	    case "Landroid/app/Service;.onCreate:()V" |
	    		 "Landroid/app/Service;.onStart:(Landroid/content/Intent;I)V" |
	    		 "Landroid/app/Service;.onStartCommand:(Landroid/content/Intent;II)I" |
	    		 "Landroid/app/Service;.onBind:(Landroid/content/Intent;)Landroid/os/IBinder;" |
	    		 "Landroid/app/Service;.onRebind:(Landroid/content/Intent;)V" |
	    		 "Landroid/app/Service;.onUnbind:(Landroid/content/Intent;)Z" |
	    		 "Landroid/app/Service;.onDestroy:()V" |
	    		 "Landroid/content/BroadcastReceiver;.onReceive:(Landroid/content/Context;Landroid/content/Intent;)V" |
	    		 "Landroid/content/ContentProvider;.onCreate:()Z" |
	    		 "Landroid/os/AsyncTask;.execute:([Ljava/lang/Object;)Landroid/os/AsyncTask;"=> true
	    case _ => false
	  }
	}
	
	def doLifecycleMethodCall(s : ISet[RFAFact], p : JawaProcedure, args : List[String], retVars : Seq[String], currentContext : Context) : ISet[RFAFact] = {
	  var newFacts = isetEmpty[RFAFact]
	  p.getSignature match{
	    case "Landroid/app/Service;.onCreate:()V" =>
	    case "Landroid/app/Service;.onStart:(Landroid/content/Intent;I)V" =>
	    case "Landroid/app/Service;.onStartCommand:(Landroid/content/Intent;II)I" =>
	    case "Landroid/app/Service;.onBind:(Landroid/content/Intent;)Landroid/os/IBinder;" =>
	    case "Landroid/app/Service;.onRebind:(Landroid/content/Intent;)V" =>
	    case "Landroid/app/Service;.onUnbind:(Landroid/content/Intent;)Z" =>
	    case "Landroid/app/Service;.onDestroy:()V" =>
	    case "Landroid/content/BroadcastReceiver;.onReceive:(Landroid/content/Context;Landroid/content/Intent;)V" =>
	    case "Landroid/content/ContentProvider;.onCreate:()Z"=>
	    case "Landroid/os/AsyncTask;.execute:([Ljava/lang/Object;)Landroid/os/AsyncTask;" =>
	    case _ =>
	  }
	  s ++ newFacts
	}
	
}