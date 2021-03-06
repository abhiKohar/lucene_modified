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
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermContext;
import org.apache.lucene.search.BoostQuery;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;

/**
 * Counterpart of {@link BoostQuery} for spans.
 */
public final class SpanBoostQuery extends SpanQuery {

  private final SpanQuery query;
  private final float boost;
  boolean isNested=false;

  /** Sole constructor: wrap {@code query} in such a way that the produced
   *  scores will be boosted by {@code boost}. */
  public SpanBoostQuery(SpanQuery query, float boost) {
    //query.
    this.query = Objects.requireNonNull(query);

    this.boost = boost;
    String tmp = this.query.toString();
    String [] cnt = tmp.split(Pattern.quote("^"));
    if (cnt.length>2)
    {
      this.isNested = true;

    }

  }

  /**
   * Return the wrapped {@link SpanQuery}.
   */
  public SpanQuery getQuery() {
    return query;
  }

  /**
   * Return the applied boost.
   */
  public float getBoost() {
    return boost;
  }

  @Override
  public boolean equals(Object other) {
    return sameClassAs(other) &&
           equalsTo(getClass().cast(other));
  }
  
  private boolean equalsTo(SpanBoostQuery other) {
    return query.equals(other.query) && 
           Float.floatToIntBits(boost) == Float.floatToIntBits(other.boost);
  }

  @Override
  public int hashCode() {
    int h = classHash();
    h = 31 * h + query.hashCode();
    h = 31 * h + Float.floatToIntBits(boost);
    return h;
  }

  @Override
  public Query rewrite(IndexReader reader) throws IOException {
    if (boost == 1f) {
      return query;
    }

    final SpanQuery rewritten = (SpanQuery) query.rewrite(reader);
    if (query != rewritten) {
      return new SpanBoostQuery(rewritten, boost);
    }

    if (query.getClass() == SpanBoostQuery.class) {
      SpanBoostQuery in = (SpanBoostQuery) query;
      return new SpanBoostQuery(in.query, boost * in.boost);
    }

    return super.rewrite(reader);
  }

  @Override
  public String toString(String field) {
    StringBuilder builder = new StringBuilder();
    builder.append("(");
    builder.append(query.toString(field));
    builder.append(")^");
    builder.append(boost);
    return builder.toString();
  }

  @Override
  public String getField() {
    return query.getField();
  }

  @Override
  public SpanWeight createWeight(IndexSearcher searcher, boolean needsScores) throws IOException {
    final SpanWeight weight = query.createWeight(searcher, needsScores);
    needsScores=true;//remove need to be parameter this for debugging purposes.
    if (needsScores == false) {//this is nested became true because of above params ^ regex -> not passed span near query, although passed everything around., look for that.
      return weight;
    }
    Map<Term, TermContext> terms = new TreeMap<>();
    weight.extractTermContexts(terms);// here lies the problem subweights simweight ==1
    weight.normalize(1f, boost);// in normalize changes subweights sim weight boost to 1
    return new SpanWeight(this, searcher, terms,isNested) {
      
      @Override
      public void extractTerms(Set<Term> terms) {
        weight.extractTerms(terms);
      }

      @Override
      public Explanation explain(LeafReaderContext context, int doc) throws IOException {
        return weight.explain(context, doc);
      }

      @Override
      public float getValueForNormalization() throws IOException {
        return weight.getValueForNormalization();
      }

      @Override
      public void normalize(float norm, float boost) {
        weight.normalize(norm, SpanBoostQuery.this.boost * boost);
      }
      
      @Override
      public Spans getSpans(LeafReaderContext ctx, Postings requiredPostings) throws IOException {
        if(isNested) {
          Integer [] tmp = new Integer[1];
          tmp[0]=1;
          return weight.getSpans(ctx, requiredPostings, tmp);


        }
        else{
          return weight.getSpans(ctx, requiredPostings);
        }
      }

      @Override
      public SpanScorer scorer(LeafReaderContext context) throws IOException {
        if (isNested==true) {// here when is Nested is true change the span windwo length os subspans
          Integer [] weigh = new Integer[1];
          weigh[0]=1;
          return weight.scorer(context);
        }
        else
        return weight.scorer(context);
      }

      @Override
      public void extractTermContexts(Map<Term,TermContext> contexts) {
        weight.extractTermContexts(contexts);
      }
    };
  }

}
