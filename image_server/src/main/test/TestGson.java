import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.HashMap;

class Hero{
    public String name;
    public String skill1;
    public String skill2;
    public String skill3;
    public String skill4;
}

public class TestGson {
    public static void main(String[] args) {
       /* HashMap<String,Object> hashMap=new HashMap<String, Object>();
        hashMap.put("name","李青");
        hashMap.put("skill1","天音波");
        hashMap.put("skill2","金钟罩");
        hashMap.put("skill3","天雷破");
        hashMap.put("skill4","神龙摆尾");
        */

       Hero hero=new Hero();
       hero.name="李青";
       hero.skill1="天音波";
       hero.skill2="金钟罩";
       hero.skill3="天雷破";
       hero.skill4="神龙摆尾";

        //通过map转成JSON结构的字符串
        //1.先创建一个gson对象
        Gson gson= new GsonBuilder().create();
        //2.使用toJson方法把键值对结构转为JSON字符串
        String str=gson.toJson(hero);
        System.out.println(str);
    }
}
