/*
* https://leetcode.com/problems/single-number
* https://leetcode.com/problems/single-number-ii/
* https://leetcode.com/problems/single-number-iii/
*
* 2015/12/27
*/


#include <stdlib.h>

int singleNumber(int* nums, int numsSize) {
    int ans, i;

    for (i = ans = 0; i < numsSize; ++i) {
        ans ^= nums[i];
    }
    return ans;
}

int singleNumber2(int* nums, int numsSize) {
    int ans, i;

    return ans;
}

/**
* Return an array of size *returnSize.
* Note: The returned array must be malloced, assume caller calls free().
*/
int* singleNumber3(int* nums, int numsSize, int *returnSize) {
    int i, xor, diffAt;
    int *ans = (int*)malloc(sizeof(int) * 2);
    *returnSize = 2;
    for (i = xor = 0; i < numsSize; ++i) {
        xor ^= nums[i];
    }
    diffAt = xor & ((~xor) +1); // xor & -xor;
    for (i = *ans = 0; i < numsSize; ++i) {
        if (diffAt & nums[i]) {
            *ans ^= nums[i];
        }
    }
    ans[1] = xor ^ *ans;
    return ans;
}
