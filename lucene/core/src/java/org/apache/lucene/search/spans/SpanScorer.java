/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.lucene.search.spans;
import java.io.IOException;

import java.io.IOException;
import java.util.Objects;

import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TwoPhaseIterator;
import org.apache.lucene.search.similarities.Similarity;

/**
 * A basic {@link Scorer} over {@link Spans}.
 * @lucene.experimental
 */
public class SpanScorer extends Scorer {

  protected  Spans spans;
  protected final Similarity.SimScorer docScorer;
  boolean boosted = false;
  Integer[] weigh ;
  /** accumulated sloppy freq (computed in setFreqCurrentDoc) */
  private float freq;
  /** number of matches (computed in setFreqCurrentDoc) */
  private int numMatches;
  private int lastScoredDoc = -1; // last doc we called setFreqCurrentDoc() for

  /** Sole constructor. */
  public SpanScorer(SpanWeight weight, Spans spans, Similarity.SimScorer docScorer,Integer... weigh) {// nobody is sending weigh cahnged design
    super(weight);
    //try{
      if (spans instanceof  NearSpansUnordered  )
      {
        NearSpansUnordered q = (NearSpansUnordered) spans;
        if (q.spanboost) {
          this.boosted = q.spanboost;
          this.weigh = weigh;
        }
      }
    //}
//    catch(IOException e){
//      e.printStackTrace();
//
//    }

    this.spans = Objects.requireNonNull(spans);
    this.docScorer = docScorer;
  }

  /** return the Spans for this Scorer **/
  public Spans getSpans() {
    return spans;
  }

  @Override
  public int docID() {
    return spans.docID();
  }

  @Override
  public DocIdSetIterator iterator() {
    return spans;
  }

  @Override
  public TwoPhaseIterator twoPhaseIterator() {
    return spans.asTwoPhaseIterator();
  }

  /**
   * Score the current doc. The default implementation scores the doc
   * with the similarity using the slop-adjusted {@link #freq}.
   */
  protected float scoreCurrentDoc() throws IOException {
    assert docScorer != null : getClass() + " has a null docScorer!";
    return docScorer.score(docID(), freq);
  }

  /**
   * Sets {@link #freq} and {@link #numMatches} for the current document.
   * <p>
   * This will be called at most once per document.
   */


  protected final float sumweightedSubspans() throws IOException {

  float sum = 0.0f;
  // traverse spans recursively and sum them up ; in current case just need to sum up two ueries do that firse

    return sum;

  }

  protected final void setFreqCurrentDoc() throws IOException {
    freq = 0.0f;
    numMatches = 0;

    spans.doStartCurrentDoc();

    if (this.boosted)//check when boosted is correct or not. this.spans.        --> weight.parentQuery.clauses [0]/[1] get boose  --> spans.subspans [0]/[1]//order fosmt change it always the same//checkc PAttern.boost is not affecting this.boosted (make it false.)
    {
      if(spans instanceof NearSpansUnordered)
      {
        NearSpansUnordered q = (NearSpansUnordered) spans;
        SpanNearQuery.SpanNearWeight w;
        if(weight instanceof SpanNearQuery.SpanNearWeight)
        {
          //w = (SpanNearQuery.SpanNearWeight) weight;

          int i = q.subSpans.length;
          for (int j =0;j<i;j++)
          {
            Query tmp = weight.getQuery();
            //tmp = "";
            System.out.println("yes");
            SpanNearQuery tmp2 = (SpanNearQuery) tmp;
            Object t = tmp2.clauses.get(j);
            SpanBoostQuery sb = (SpanBoostQuery) t;

            freq += 1.0/(q.subSpans[j].width() /(sb.getBoost()+1.0));
          }
        }

      }

//      int i = 0;//spans["subSpans"].length;
//
//      for (int j=0;j<i;j++)
//      {
//        freq += 0;
//        //freq += spans.;
//      }
      //this.spans.spanWindow.totalSpanLength =this.spans.spanWindow.totalSpanLength /(this.spans.spanboost + 1.0); // cannot access spanwindow :/


      return ;
    }
//set weight factor == true here. here it gets all the spans ans need to combine them usign weights  they maybe multiple nested spans -> not doing for that. Just for two span queries, that is what we do
    //right now. treee like computation to affect span query
    assert spans.startPosition() == -1 : "incorrect initial start position, " + spans;
    assert spans.endPosition() == -1 : "incorrect initial end position, " + spans;
    int prevStartPos = -1;
    int prevEndPos = -1;

    int startPos = spans.nextStartPosition();
    assert startPos != Spans.NO_MORE_POSITIONS : "initial startPos NO_MORE_POSITIONS, " + spans;
    do {
      assert startPos >= prevStartPos;
      int endPos = spans.endPosition();
      assert endPos != Spans.NO_MORE_POSITIONS;
      // This assertion can fail for Or spans on the same term:
      // assert (startPos != prevStartPos) || (endPos > prevEndPos) : "non increased endPos="+endPos;
      assert (startPos != prevStartPos) || (endPos >= prevEndPos) : "decreased endPos="+endPos;
      numMatches++;
      if (docScorer == null) {  // scores not required, break out here
        freq = 1;
        return;
      }
      freq += docScorer.computeSlopFactor(spans.width());
      spans.doCurrentSpans();
      prevStartPos = startPos;
      prevEndPos = endPos;
      startPos = spans.nextStartPosition();
    } while (startPos != Spans.NO_MORE_POSITIONS);

    assert spans.startPosition() == Spans.NO_MORE_POSITIONS : "incorrect final start position, " + spans;
    assert spans.endPosition() == Spans.NO_MORE_POSITIONS : "incorrect final end position, " + spans;
  }

  /**
   * Ensure setFreqCurrentDoc is called, if not already called for the current doc.
   */
  private void ensureFreq() throws IOException {
    int currentDoc = docID();
    if (this.boosted)
    {
      if (lastScoredDoc != currentDoc) {
        setFreqCurrentDoc();
        lastScoredDoc = currentDoc;
      }
    }
    else {
      if (lastScoredDoc != currentDoc) {
        setFreqCurrentDoc();
        lastScoredDoc = currentDoc;
      }
    }
  }

  @Override
  public final float score() throws IOException {
    //if this
    ensureFreq();
    return scoreCurrentDoc();
  }
//  public final float score(Integer... weigh) throws IOException {
//    ensureFreq(weigh);
//    return scoreCurrentDoc(weigh);
//  }

  @Override
  public final int freq() throws IOException {
    ensureFreq();
    return numMatches;
  }


//  public final int freq(Integer... weigh) throws IOException {
//    ensureFreq(weigh);
//    return numMatches;
//  }

  /** Returns the intermediate "sloppy freq" adjusted for edit distance
   *  @lucene.internal */
  final float sloppyFreq() throws IOException {
    ensureFreq();
    return freq;
  }

}
