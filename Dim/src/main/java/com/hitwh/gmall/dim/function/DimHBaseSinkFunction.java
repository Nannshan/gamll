package com.hitwh.gmall.dim.function;

import com.alibaba.fastjson.JSONObject;
import com.hitwh.gamll.common.bean.TableProcessDim;
import com.hitwh.gamll.common.constant.Constant;
import com.hitwh.gamll.common.util.HBaseUtil;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.sink.RichSinkFunction;
import org.apache.hadoop.hbase.client.Connection;

import java.io.IOException;

public class DimHBaseSinkFunction extends RichSinkFunction<Tuple2<JSONObject, TableProcessDim>> {
    Connection conn ;
    @Override
    public void open(Configuration parameters) throws Exception {
        conn = HBaseUtil.getConnection();
    }

    @Override
    public void close() throws Exception {
        HBaseUtil.closeConnection(conn);
    }

    @Override
    public void invoke(Tuple2<JSONObject, TableProcessDim> value, Context context) throws Exception {
        JSONObject jsonObject = value.f0;
        TableProcessDim dim = value.f1;
        String type = jsonObject.getString("type");
        // insert update delete bootstrap-insert 剩余两种已经被过滤
        JSONObject data = jsonObject.getJSONObject("data");
        if("delete".equals(type)){
            //删除维度表数据
            delete(data, dim);
        }else{
            //覆盖写入维度表数据
            put(data, dim);
        }
    }
    public void put(JSONObject data, TableProcessDim dim){
        String sinkTable = dim.getSinkTable();
        String sinkRowKeyName = dim.getSinkRowKey();
        String sinkRowKeyValue = data.getString(sinkRowKeyName);
        String sinkFamily = dim.getSinkFamily();
        try {
            HBaseUtil.putCells(conn, Constant.HBASE_NAMESPACE, sinkTable, sinkRowKeyValue, sinkFamily, data);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public void delete(JSONObject data, TableProcessDim dim){
        String sinkTable = dim.getSinkTable();
        String sinkRowKeyName = dim.getSinkRowKey();
        String sinkRowKeyValue = data.getString(sinkRowKeyName);
        try {
            HBaseUtil.deleteCells(conn,Constant.HBASE_NAMESPACE,sinkTable,sinkRowKeyValue);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
