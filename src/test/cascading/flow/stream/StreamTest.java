/*
 * Copyright (c) 2007-2011 Concurrent, Inc. All Rights Reserved.
 *
 * Project and contact information: http://www.cascading.org/
 *
 * This file is part of the Cascading project.
 *
 * Cascading is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Cascading is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Cascading.  If not, see <http://www.gnu.org/licenses/>.
 */

package cascading.flow.stream;

import java.util.ArrayList;
import java.util.List;

import cascading.test.PlatformTest;
import junit.framework.TestCase;

/**
 *
 */
@PlatformTest(platforms = {"none"})
public class StreamTest extends TestCase
  {
  public StreamTest()
    {
    }

  public void testStageStream()
    {
    List<String> values = new ArrayList<String>();

    for( int i = 0; i < 10; i++ )
      values.add( "value" );

    TestSourceStage source = new TestSourceStage<String>( values );

    CountingItemStage lhsStage1 = new CountingItemStage<String, String>();
    CountingItemStage lhsStage2 = new CountingItemStage<String, String>();

    CountingItemStage rhsStage1 = new CountingItemStage<String, String>();
    CountingItemStage rhsStage2 = new CountingItemStage<String, String>();

    CountingItemStage mergeStage = new CountingItemStage<String, String>();

    TestSinkStage lhsSink = new TestSinkStage<String>();
    TestSinkStage rhsSink = new TestSinkStage<String>();

    StreamGraph graph = new StreamGraph();

    graph.addHead( source );

    graph.addPath( source, lhsStage1 );
    graph.addPath( lhsStage1, mergeStage );
    graph.addPath( mergeStage, lhsStage2 );
    graph.addPath( lhsStage2, lhsSink );
    graph.addTail( lhsSink );

    graph.addPath( source, rhsStage1 );
    graph.addPath( rhsStage1, mergeStage );
    graph.addPath( mergeStage, rhsStage2 );
    graph.addPath( rhsStage2, rhsSink );
    graph.addTail( rhsSink );

    graph.bind();

    graph.prepare();

    source.receiveFirst( null );

    graph.cleanup();

    assertPrepareCleanup( lhsStage1 );
    assertPrepareCleanup( rhsStage1 );

    assertEquals( values.size(), lhsStage1.getReceiveCount() );
    assertEquals( values.size(), rhsStage1.getReceiveCount() );

    assertEquals( values.size() * 2, lhsStage2.getReceiveCount() );
    assertEquals( values.size() * 2, rhsStage2.getReceiveCount() );

    assertEquals( values.size() * 2, lhsSink.getResults().size() );
    assertEquals( values.size() * 2, rhsSink.getResults().size() );
    }

  public void testGateStageStream()
    {
    List<String> values = new ArrayList<String>();

    for( int i = 0; i < 10; i++ )
      values.add( "value" );

    TestSourceStage source = new TestSourceStage<String>( values );

    Gate gate = new TestGate();
    CountingItemStage stage = new CountingItemStage<String, String>();

    TestSinkStage sink = new TestSinkStage<String>();

    StreamGraph graph = new StreamGraph();

    graph.addHead( source );

    graph.addPath( source, gate );
    graph.addPath( gate, stage );
    graph.addPath( stage, sink );

    graph.addTail( sink );

    graph.bind();

    graph.prepare();

    source.receiveFirst( null );

    graph.cleanup();

    assertPrepareCleanup( stage );

    assertEquals( values.size(), stage.getReceiveCount() );
    assertEquals( values.size(), sink.getResults().size() );
    }

  public void testGateGroupStream()
    {
    List<String> values = new ArrayList<String>();

    for( int i = 0; i < 10; i++ )
      values.add( "value" );

    TestSourceStage source = new TestSourceStage<String>( values );

    Gate<String, String> gate = new TestGate<String, String>();
    CountingCollectStage stage1 = new CountingCollectStage<String, String>();
    CountingCollectStage stage2 = new CountingCollectStage<String, String>();

    TestSinkStage sink = new TestSinkStage<String>();

    StreamGraph graph = new StreamGraph();

    graph.addHead( source );

    graph.addPath( source, gate );
    graph.addPath( gate, stage1 );
    graph.addPath( stage1, stage2 );
    graph.addPath( stage2, sink );

    graph.addTail( sink );

    graph.bind();

    graph.prepare();

    source.receiveFirst( null );

    graph.cleanup();

    assertPrepareCleanup( stage1 );

    assertEquals( values.size(), stage1.getReceiveCount() );
    assertEquals( 1, stage1.getStartCount() );
    assertEquals( 1, stage1.getCompleteCount() );

    assertEquals( values.size(), stage2.getReceiveCount() );
    assertEquals( 1, stage2.getStartCount() );
    assertEquals( 1, stage2.getCompleteCount() );

    assertEquals( 1, sink.getResults().size() );
    }

  public void testMergeGateGroupStream()
    {
    List<String> values = new ArrayList<String>();

    for( int i = 0; i < 10; i++ )
      values.add( "value" );

    TestSourceStage source1 = new TestSourceStage<String>( values );
    TestSourceStage source2 = new TestSourceStage<String>( values );

    Gate<String, String> gate = new TestGate<String, String>();
    CountingCollectStage stage1 = new CountingCollectStage<String, String>();
    CountingCollectStage stage2 = new CountingCollectStage<String, String>();

    TestSinkStage sink = new TestSinkStage<String>();

    StreamGraph graph = new StreamGraph();

    graph.addHead( source1 );
    graph.addHead( source2 );

    graph.addPath( source1, gate );
    graph.addPath( source2, gate );
    graph.addPath( gate, stage1 );
    graph.addPath( stage1, stage2 );
    graph.addPath( stage2, sink );

    graph.addTail( sink );

    graph.bind();

    graph.prepare();

    source1.receiveFirst( null );
    source2.receiveFirst( null );

    graph.cleanup();

    assertPrepareCleanup( stage1 );

    assertEquals( values.size() * 2, stage1.getReceiveCount() );
    assertEquals( 1, stage1.getStartCount() );
    assertEquals( 1, stage1.getCompleteCount() );

    assertEquals( values.size() * 2, stage2.getReceiveCount() );
    assertEquals( 1, stage2.getStartCount() );
    assertEquals( 1, stage2.getCompleteCount() );

    assertEquals( 1, sink.getResults().size() );
    }

  private void assertPrepareCleanup( CountingItemStage stage )
    {
    assertEquals( 1, stage.getPrepareCount() );
    assertEquals( 1, stage.getCleanupCount() );
    }

  private void assertPrepareCleanup( CountingCollectStage stage )
    {
    assertEquals( 1, stage.getPrepareCount() );
    assertEquals( 1, stage.getCleanupCount() );
    }
  }