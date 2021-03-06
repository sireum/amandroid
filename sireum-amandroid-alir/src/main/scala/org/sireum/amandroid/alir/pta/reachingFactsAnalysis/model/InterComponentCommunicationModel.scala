/*******************************************************************************
 * Copyright (c) 2013 - 2016 Fengguo Wei and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Detailed contributors are listed in the CONTRIBUTOR.md
 ******************************************************************************/
package org.sireum.amandroid.alir.pta.reachingFactsAnalysis.model

import org.sireum.amandroid.AndroidConstants
import org.sireum.util._
import org.sireum.jawa.JawaMethod
import org.sireum.jawa.alir.Context
import org.sireum.jawa.alir.pta.reachingFactsAnalysis._
import org.sireum.amandroid.parser.UriData
import org.sireum.jawa.JawaClass
import org.sireum.jawa.alir.pta.Instance
import java.net.URI
import org.sireum.amandroid.alir.pta.reachingFactsAnalysis.IntentHelper
import org.sireum.jawa.alir.pta.PTAResult
import org.sireum.jawa.alir.pta.VarSlot
import org.sireum.jawa.JawaType
import org.sireum.amandroid.Apk
import org.sireum.jawa.Signature
import org.sireum.jawa.Global

/**
 * @author <a href="mailto:fgwei@k-state.edu">Fengguo Wei</a>
 * @author <a href="mailto:sroy@k-state.edu">Sankardas Roy</a>
 */ 
object InterComponentCommunicationModel {
  final val TITLE = "InterComponentCommunicationModel"
  def isIccOperation(proc: Signature): Boolean = {
    var flag = false
    AndroidConstants.getIccMethods.foreach{
      item =>
        if(proc.getSubSignature == item)
         flag = true
    }
    flag
  }

  def doIccCall(global: Global, apk: Apk, s: PTAResult, calleeSig: Signature, args: List[String], retVars: Seq[String], currentContext: Context): (ISet[RFAFact], ISet[JawaMethod]) = {
    require(args.size > 1)
    val intentSlot = VarSlot(args(1), false, true)
    val intentValues = s.pointsToSet(intentSlot, currentContext)
    val intentcontents = IntentHelper.getIntentContents(s, intentValues, currentContext)
    val compType: AndroidConstants.CompType.Value = AndroidConstants.getIccCallType(calleeSig.getSubSignature)
    val comMap = IntentHelper.mappingIntents(global, apk, intentcontents, compType)
    val targets: MSet[JawaMethod] = msetEmpty
    comMap.foreach{
      case (_, comTypes) =>
        comTypes.foreach{
          case (comType, _) =>
            val com = global.getClassOrResolve(comType)
            com.getMethod(AndroidConstants.MAINCOMP_ENV_SUBSIG) match{
              case Some(r) => targets += r
              case None => 
                com.getMethod(AndroidConstants.COMP_ENV_SUBSIG) match{
                  case Some(r) => targets += r
                  case None => 
                }
            }
        }
    }
    (isetEmpty, targets.toSet)
  }

}
