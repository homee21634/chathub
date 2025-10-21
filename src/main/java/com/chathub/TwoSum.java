package com.chathub;

import java.util.HashMap;
import java.util.Map;

/**
 * LeetCode 1. Two Sum
 * 難度：Easy
 *
 * 題目：給定一個整數數組 nums 和一個目標值 target，
 * 找出數組中和為目標值的兩個數的索引。
 */
public class TwoSum {

    /**
     * 使用 HashMap 解法
     * 時間複雜度：O(n)
     * 空間複雜度：O(n)
     */
    public int[] twoSum(int[] nums, int target) {
        // 使用 HashMap 存儲已遍歷的數字及其索引
        Map<Integer, Integer> map = new HashMap<>();

        for (int i = 0; i < nums.length; i++) {
            int complement = target - nums[i];

            // 如果找到complement，返回兩個索引
            if (map.containsKey(complement)) {
                return new int[] { map.get(complement), i };
            }

            // 將當前數字和索引存入map
            map.put(nums[i], i);
        }

        // 如果沒有找到解，返回空數組
        throw new IllegalArgumentException("No two sum solution");
    }

    /**
     * 測試用例
     */
    public static void main(String[] args) {
        TwoSum solution = new TwoSum();

        // 測試案例 1
        int[] nums1 = {2, 7, 11, 15};
        int target1 = 9;
        int[] result1 = solution.twoSum(nums1, target1);
        System.out.println("輸入: nums = [2,7,11,15], target = 9");
        System.out.println("輸出: [" + result1[0] + ", " + result1[1] + "]");
        System.out.println("說明: nums[0] + nums[1] = 2 + 7 = 9\n");

        // 測試案例 2
        int[] nums2 = {3, 2, 4};
        int target2 = 6;
        int[] result2 = solution.twoSum(nums2, target2);
        System.out.println("輸入: nums = [3,2,4], target = 6");
        System.out.println("輸出: [" + result2[0] + ", " + result2[1] + "]");
        System.out.println("說明: nums[1] + nums[2] = 2 + 4 = 6\n");

        // 測試案例 3
        int[] nums3 = {3, 3};
        int target3 = 6;
        int[] result3 = solution.twoSum(nums3, target3);
        System.out.println("輸入: nums = [3,3], target = 6");
        System.out.println("輸出: [" + result3[0] + ", " + result3[1] + "]");
        System.out.println("說明: nums[0] + nums[1] = 3 + 3 = 6");
    }
}