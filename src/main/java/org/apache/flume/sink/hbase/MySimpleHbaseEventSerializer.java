/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.flume.sink.hbase;

import com.google.common.base.Charsets;
import org.apache.flume.Context;
import org.apache.flume.Event;
import org.apache.flume.FlumeException;
import org.apache.flume.conf.ComponentConfiguration;
import org.apache.hadoop.hbase.client.Increment;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Row;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * 自定义flume和hbase sink
 * 需要解决的问题：
 *  1、读到我们配置的列名有哪些
 *  2、得要知道如何去拆分我们的数据
 *
 *  如果要想在这个类中获取自定义的配置项信息，那么该配置项必须要在serializer.*
 *  的后面（*）来指定
 *
 */
public class MySimpleHbaseEventSerializer implements HbaseEventSerializer {
  private String rowPrefix;
  private byte[] incrementRow;
  private byte[] columnFamily;
  private byte[] cf;
  private String plCol;
  private byte[] incCol;
  private byte[] payload;
  private String seperator;

  public MySimpleHbaseEventSerializer() {
  }

  /**
   * 获得配置文件中的配置项
   * @param context
   */
  @Override
  public void configure(Context context) {
    rowPrefix = context.getString("rowPrefix", "default");
    incrementRow =
        context.getString("incrementRow", "incRow").getBytes(Charsets.UTF_8);
    String suffix = context.getString("suffix", "uuid");
    //datetime,userid,searchname,retorder,cliorder,cliurl
    String payloadColumn = context.getString("payloadColumn", "pCol");
    String incColumn = context.getString("incrementColumn", "iCol");
    String seperator = context.getString("seperator", ",");

    String cf = context.getString("columnFamily", "cf1");

    if (payloadColumn != null && !payloadColumn.isEmpty()) {
      plCol = payloadColumn;//pCol
      System.out.println("==============>payloadColumn: " + plCol);
    }
    if (incColumn != null && !incColumn.isEmpty()) {
      incCol = incColumn.getBytes(Charsets.UTF_8);//iCol
    }
    if(seperator != null && !seperator.isEmpty()) {
      this.seperator = seperator;
      System.out.println("==============>seperator: " + seperator);
    }
    if(cf != null && !cf.isEmpty()) {
      this.columnFamily = cf.getBytes(Charsets.UTF_8);
      System.out.println("==============>columnFamily: " + cf);
    }
  }

  @Override
  public void configure(ComponentConfiguration conf) {
  }

  @Override
  public void initialize(Event event, byte[] cf) {
    this.payload = event.getBody();//读取到的数据
    this.cf = cf;
  }

    /**
     * 把数据由channel写入到hbase的操作就在这里面实现
   * @return
     * @throws FlumeException
   */
  @Override
  public List<Row> getActions() throws FlumeException {
    List<Row> actions = new LinkedList<Row>();
    if (plCol != null) {//pCol
      byte[] rowKey = null;
      try {
        //数据在payload中，列信息在plCol中，这二者需要一一对应

        String[] columns = this.plCol.split(",");
        System.out.println("---------------->columns: " + Arrays.toString(columns));
        String[] dataArray = new String(this.payload).split(this.seperator);
        System.out.println("---------------->columns: " + Arrays.toString(dataArray));

        if(columns == null || dataArray == null || columns.length != dataArray.length) {
          return actions;
        } else {
          String userid = dataArray[1];
          String time = dataArray[0];
          Put put = null;
          for (int i = 0; i < columns.length; i++) {
            String column = columns[i];
            String data = dataArray[i];
            put = new Put(SimpleRowKeyGenerator.getUserIdAndTimeKey(time, userid));
            put.addColumn(columnFamily, column.getBytes(), data.getBytes());
            actions.add(put);
          }
        }
      } catch (Exception e) {
        throw new FlumeException("Could not get row key!", e);
      }

    }
    return actions;
  }

  @Override
  public List<Increment> getIncrements() {
    List<Increment> incs = new LinkedList<Increment>();
    if (incCol != null) {
      Increment inc = new Increment(incrementRow);
      inc.addColumn(cf, incCol, 1);
      incs.add(inc);
    }
    return incs;
  }

  @Override
  public void close() {
  }
}
