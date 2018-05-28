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
import org.apache.lucene.search.similarities.*;
import org.apache.lucene.analysis.MockAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.RandomIndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.CheckHits;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;
//import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.util.LuceneTestCase;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.ScoreDoc;

public class TestSpanBoostQuery extends LuceneTestCase {

  private IndexSearcher searcher;
  private IndexReader reader;
  private Directory directory;

  public static final String field = "field";

  @Override
  public void setUp() throws Exception {
    super.setUp();
    directory = newDirectory();
    RandomIndexWriter writer= new RandomIndexWriter(random(), directory, newIndexWriterConfig(new MockAnalyzer(random())).setMergePolicy(newLogMergePolicy()));
    for (int i = 0; i < docFields.length; i++) {
      Document doc = new Document();
      doc.add(newTextField(field, docFields[i], Field.Store.YES));
      writer.addDocument(doc);
    }
    writer.forceMerge(1);
    reader = writer.getReader();
    writer.close();
    searcher = newSearcher(getOnlyLeafReader(reader));
  }

  @Override
  public void tearDown() throws Exception {
    reader.close();
    directory.close();
    super.tearDown();
  }

  private String[] docFields = {
      "w1 w2 w3 w4 w5",//"w1 w2 w3 w4 w5 w1",//"w1 w2 w3 w4 w5",
      //"w1 w3 w2 w3",
      //"w1 xx w2 yy w3",
      //"w1 w3 xx w2 yy w3",
      "u2 u2 u1",
      "u2 xx u2 u1",
      "u2 u2 xx u1",
      "u2 xx u2 yy u1",
      "u2 xx u1 u2",
      "u2 u1 xx u2",
      "u1 u2 xx u2",
      "t1 t2 t1 t3 t2 t3",
      "s2 s1 s1 xx xx s2 xx s2 xx s1 xx xx xx xx xx s2 xx",
      "r1 s11",
      "r1 s21"
  };

  private void checkHits(Query query, int[] results) throws IOException {
    CheckHits.checkHits(random(), query, field, searcher, results);
  }



  public void testEquals() {
    final float boost = random().nextFloat() * 3 - 1;
    SpanTermQuery q = new SpanTermQuery(new Term("foo", "bar"));
    SpanBoostQuery q1 = new SpanBoostQuery(q, boost);
    SpanBoostQuery q2 = new SpanBoostQuery(q, boost);
    assertEquals(q1, q2);
    assertEquals(q1.getBoost(), q2.getBoost(), 0f);
 
    float boost2 = boost;
    while (boost == boost2) {
      boost2 = random().nextFloat() * 3 - 1;
    }
    SpanBoostQuery q3 = new SpanBoostQuery(q, boost2);
    assertFalse(q1.equals(q3));
    assertFalse(q1.hashCode() == q3.hashCode());
  }

  public void testToString() {
    assertEquals("(foo:bar)^2.0", new SpanBoostQuery(new SpanTermQuery(new Term("foo", "bar")), 2).toString());
    SpanOrQuery bq = new SpanOrQuery(
        new SpanTermQuery(new Term("foo", "bar")),
        new SpanTermQuery(new Term("foo", "baz")));
    assertEquals("(spanOr([foo:bar, foo:baz]))^2.0", new SpanBoostQuery(bq, 2).toString());
// test boosting for nested queries
    SpanTermQuery q11 =  new SpanTermQuery(new Term(field, "w1"));
    SpanTermQuery q12 =  new SpanTermQuery(new Term(field, "w2"));
    SpanTermQuery q13 =  new SpanTermQuery(new Term(field, "w3"));
    SpanNearQuery q1 = new SpanNearQuery(new SpanQuery[] { q11,q12}, 10, false);
    //q1.setBoost(2.0);
    SpanBoostQuery bq1 = new SpanBoostQuery(q1, 4);
    SpanNearQuery q2 = new SpanNearQuery(new SpanQuery[] { q12,q13}, 10, false);
    //q2.setBoost(3.0);
    SpanBoostQuery bq2 = new SpanBoostQuery(q2, 10);
//    IllegalArgumentException expected = expectThrows(IllegalArgumentException.class, () -> {
//      new SpanNearQuery(new SpanQuery[] { q1, q2 }, 10, true);
//    });
//    assertTrue(expected.getMessage().contains("must have same field"));

//   try {
//     System.out.print(1);//setUp();
//   }
//   catch(IOException e) {
//     e.printStackTrace();
//   }
    SpanWeight tmp;
    boolean [] isNestedA = new boolean [1];
    isNestedA[0]=true;
    SpanNearQuery nspan =  new SpanNearQuery(new SpanQuery[] { bq1, bq2 }, 10, false,isNestedA);// add the isNested true here, easiest way to pass, user has to tell thta scores need to be boosted
    SpanBoostQuery bnspan = new SpanBoostQuery(nspan, 5);
//    try {
//      //SpanWeight w = bnspan.createWeight(searcher, true);
//    System.out.println(1);
//    }
//
//    catch(IOException e) {
//      e.printStackTrace();
//    }
    Sort sort = new Sort(new SortField[] { SortField.FIELD_SCORE, SortField.FIELD_DOC });
    TopDocs t_docs ;
    //SpanScorer s  = bnspan.createWeight(searcher, true).scorer(reader.getto().);
    String str_bnspan = bnspan.toString();
    //bnspan.scorer()
    searcher.setSimilarity(new BM25Similarity());
    try {
       t_docs =  searcher.search(bnspan, 5,  sort, true , true);
      System.out.println("yes");
       System.out.println(t_docs.totalHits);
      System.out.println(t_docs.getMaxScore());
      System.out.println("yes 2");
      //TopDocs results = searcher.search(bnspan, filter, 10); // Apply filter here.
      ScoreDoc[] hits = t_docs.scoreDocs;
      for(ScoreDoc hit : hits)
      {
        System.out.println(searcher.explain(bnspan, hit.doc)); // Filter won't affect this either way.
      }
    }
    catch(IOException e) {
      e.printStackTrace();
    }

      System.out.print(1);
  }
//Creates a new Similarity.SimScorer to score matching documents from a segment of the inverted index.//changed for BM25 scorer random scoring to increase doc exposure
}
