package com.twitter.scalding

import cascading.stats.{ CascadeStats, CascadingStats }
import cascading.flow.FlowProcess
import cascading.stats.FlowStats

import scala.collection.JavaConverters._

/**
 * Wrapper around a FlowProcess useful, for e.g. incrementing counters.
 */
object Stats extends java.io.Serializable {
  private var flowProcess : Option[FlowProcess[_]] = None
  private var flowStats: Option[FlowStats] = None
  private var cascadeStats: Option[CascadeStats] = None

  // This is the group that we assign all custom counters to
  val ScaldingGroup = "Scalding Custom"

  def setFlowProcess(fp: FlowProcess[_]) = { flowProcess = Some(fp) }

  def setFlowStats(fs: FlowStats) = { flowStats = Some(fs) }

  def setCascadeStats(cs: CascadeStats) = { cascadeStats = Some(cs) }

  private[this] def statsClass: Option[CascadingStats] = (cascadeStats, flowStats) match {
    case (Some(_), _) => cascadeStats
    case (_, Some(_)) => flowStats
    case _ => None
  }

  // When getting a counter value, cascadeStats takes precedence (if set) and
  // flowStats is used after that. Returns None if neither is defined.
  def getCounterValue(counter: String, group: String = ScaldingGroup): Option[Long] =
    statsClass.map { _.getCounterValue(ScaldingGroup, counter) }

  // Returns a map of all custom counter names and their counts.
  def getAllCustomCounters: Map[String, Long] = {
    val counts = for {
      s <- statsClass.toSeq
      counter <- s.getCountersFor(ScaldingGroup).asScala
      value <- getCounterValue(counter).toSeq
    } yield (counter, value)
    counts.toMap
  }

  def incrementCounter(name: String, amount: Long = 1L, group: String = ScaldingGroup): Unit =
    flowProcess.foreach { _.increment(group, name, amount) }

  // Use this if a map or reduce phase takes a while before emitting tuples.
  def keepAlive: Unit = flowProcess.foreach{ _.keepAlive() }
}
