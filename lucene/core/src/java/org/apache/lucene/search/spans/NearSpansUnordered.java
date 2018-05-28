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
import java.util.List;

import org.apache.lucene.util.PriorityQueue;

/**
 * Similar to {@link NearSpansOrdered}, but for the unordered case.
 *
 * Expert:
 * Only public for subclassing.  Most implementations should not need this class
 */
public class NearSpansUnordered extends ConjunctionSpans {

  private final int allowedSlop;
   SpanTotalLengthEndPositionWindow spanWindow;
  boolean  spanboost;// add boost for each span query here and// should not cause problems divisision by zero as smoothing will be done.
  float width_boosted;// boosted width with the span weight just need to add to get sloppy frequency.

  public NearSpansUnordered(int allowedSlop, List<Spans> subSpans,boolean... isNestedA)//second param in is NEsted
  throws IOException {
    super(subSpans);

    this.allowedSlop = allowedSlop;
    this.spanWindow = new SpanTotalLengthEndPositionWindow();
    if(isNestedA.length >0)
    {
      this.spanboost = isNestedA[0]; // second term is the boost
    }
  }

  /** Maintain totalSpanLength and maxEndPosition */
  public class SpanTotalLengthEndPositionWindow extends PriorityQueue<Spans> {
    int totalSpanLength;
    int maxEndPosition;


    public SpanTotalLengthEndPositionWindow() {
      super(subSpans.length);
    }

    @Override
    protected final boolean lessThan(Spans spans1, Spans spans2) {
      return positionsOrdered(spans1, spans2);
    }

    void startDocument() throws IOException {//receive a boost
      clear();
      //spanboost = 2;//put span boost here for outer span queries
      totalSpanLength = 0;
      maxEndPosition = -1;
      for (Spans spans : subSpans) {
        assert spans.startPosition() == -1;
        spans.nextStartPosition();
        assert spans.startPosition() != NO_MORE_POSITIONS;
        add(spans);
        if (spans.endPosition() > maxEndPosition) {
          maxEndPosition = spans.endPosition();
        }
        int spanLength = spans.endPosition() - spans.startPosition();
        assert spanLength >= 0;
        totalSpanLength += spanLength;
      }
    }

    boolean nextPosition() throws IOException {
      Spans topSpans = top();
      assert topSpans.startPosition() != NO_MORE_POSITIONS;
      int spanLength = topSpans.endPosition() - topSpans.startPosition();
      int nextStartPos = topSpans.nextStartPosition();
      if (nextStartPos == NO_MORE_POSITIONS) {
        return false;
      }
      totalSpanLength -= spanLength;
      spanLength = topSpans.endPosition() - topSpans.startPosition();
      totalSpanLength += spanLength;
      if (topSpans.endPosition() > maxEndPosition) {
        maxEndPosition = topSpans.endPosition();
      }
      updateTop();
      return true;
    }

    boolean atMatch() {
      boolean res = (maxEndPosition - top().startPosition() - totalSpanLength) <= allowedSlop;
      return res;
    }
  }


  /** Check whether two Spans in the same document are ordered with possible overlap.
   * @return true iff spans1 starts before spans2
   *              or the spans start at the same position,
   *              and spans1 ends before spans2.
   */
  static boolean positionsOrdered(Spans spans1, Spans spans2) {
    assert spans1.docID() == spans2.docID() : "doc1 " + spans1.docID() + " != doc2 " + spans2.docID();
    int start1 = spans1.startPosition();
    int start2 = spans2.startPosition();
    return (start1 == start2) ? (spans1.endPosition() < spans2.endPosition()) : (start1 < start2);
  }

  @Override
  boolean twoPhaseCurrentDocMatches() throws IOException {
    // at doc with all subSpans
    spanWindow.startDocument();
    while (true) {
      if (spanWindow.atMatch()) {
        atFirstInCurrentDoc = true;
        oneExhaustedInCurrentDoc = false;
        return true;
      }
      if (! spanWindow.nextPosition()) {
        return false;
      }
    }
  }

  @Override
  public int nextStartPosition() throws IOException {
    if (atFirstInCurrentDoc) {
      atFirstInCurrentDoc = false;
      return spanWindow.top().startPosition();
    }
    assert spanWindow.top().startPosition() != -1;
    assert spanWindow.top().startPosition() != NO_MORE_POSITIONS;
    while (true) {
      if (! spanWindow.nextPosition()) {
        oneExhaustedInCurrentDoc = true;
        return NO_MORE_POSITIONS;
      }
      if (spanWindow.atMatch()) {
        return spanWindow.top().startPosition();
      }
    }
  }

  @Override
  public int startPosition() {
    assert spanWindow.top() != null;
    return atFirstInCurrentDoc ? -1
          : oneExhaustedInCurrentDoc ? NO_MORE_POSITIONS
          : spanWindow.top().startPosition();
  }

  @Override
  public int endPosition() {
    return atFirstInCurrentDoc ? -1
          : oneExhaustedInCurrentDoc ? NO_MORE_POSITIONS
          : spanWindow.maxEndPosition;
  }

  @Override
  public int width() {
    return spanWindow.maxEndPosition
         - spanWindow.top().startPosition();
  }

  @Override
  public void collect(SpanCollector collector) throws IOException {
    for (Spans spans : subSpans) {
      spans.collect(collector);
    }


  }

  public int numSpans() throws IOException {

     return subSpans.length;

  }

}
