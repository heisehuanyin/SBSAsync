package framework.testExample;

import framework.XMLStoreManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class XMLStoreTest {
    public static void main(String[] args){
        XMLStoreManager m = new XMLStoreManager("./test.xml").openTarget();
        XMLStoreManager.Page a = m.getPage("about");

        List<String> alist = a.getStringList("content");
        Map<String,String> amap = a.getStringMap("maps");
        System.out.println(alist);
        System.out.println(amap);

        ArrayList<String> ss = new ArrayList<>();
        Map<String,String> ab = new HashMap<>();
        for (int i=0; i<5; ++i){
            ss.add("String-"+i);
            ab.put("key"+i, "value"+i);
        }

        a.putStringList("content",ss);
        a.putStringMap("maps", ab);
        m.save();


    }
}
