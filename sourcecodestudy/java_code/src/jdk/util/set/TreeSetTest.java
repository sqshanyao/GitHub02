package jdk.util.set;

import java.util.HashSet;
import java.util.TreeSet;

/**
 * Created by user on "2"0"1"8/6/7.
 */
public class TreeSetTest {
    public static void main(String[] args) {
        TreeSet treeSet = new TreeSet();

        treeSet.add("2");
        treeSet.add("1");
        treeSet.add("3");

        HashSet hashSet = new HashSet();
        hashSet.add("2");
        hashSet.add("1");
        hashSet.add("3");

        System.out.println(treeSet);
        System.out.println(hashSet);
    }
}
