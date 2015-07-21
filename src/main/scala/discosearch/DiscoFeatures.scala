package discosearch

import edu.arizona.sista.processors.Document
import edu.arizona.sista.discourse.rstparser._

class DiscoFeatures {

  val featureExtractor = new RelationFeatureExtractor

  def mkFeatures(
    stack: List[DiscourseTree],
    buffer: List[DiscourseTree],
    doc: Document,
    edus: Array[Array[(Int, Int)]],
    corpusStats: CorpusStats,
    label: String = ""
  ): Map[String, Seq[(String, Double)]] = {
    val stackSize = stack.size
    val stackEmpty = if (stackSize == 0) 1.0 else 0.0
    val stackGreaterThanOne = if (stackSize > 1) 1.0 else 0.0
    val bufferSize = buffer.size
    val bufferEmpty = if (bufferSize == 0) 1.0 else 0.0
    val stackFeats = if (stackSize >= 2) mkFeatures(stack(1), stack(0), doc, edus, corpusStats, label) else Nil
    val bufferFeats = if (stackSize >= 1 && bufferSize >= 1) mkFeatures(stack(0), buffer(0), doc, edus, corpusStats, label) else Nil
    Map(
      "stack" -> (("size>1", stackGreaterThanOne) :: stackFeats),
      "buffer" -> (("empty", bufferEmpty) :: bufferFeats)
    )
  }

  def mkFeatures(
    left: DiscourseTree,
    right: DiscourseTree,
    doc: Document,
    edus: Array[Array[(Int, Int)]],
    corpusStats: CorpusStats,
    label: String
  ): List[(String, Double)] = {
    val feats = featureExtractor.mkFeatures(left, right, doc, edus, corpusStats, label)
    feats.toSeq.toList
  }

}
