// Tencent is pleased to support the open source community by making ncnn available.
//
// Copyright (C) 2021 THL A29 Limited, a Tencent company. All rights reserved.
//
// Licensed under the BSD 3-Clause License (the "License"); you may not use this file except
// in compliance with the License. You may obtain a copy of the License at
//
// https://opensource.org/licenses/BSD-3-Clause
//
// Unless required by applicable law or agreed to in writing, software distributed
// under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
// CONDITIONS OF ANY KIND, either express or implied. See the License for the
// specific language governing permissions and limitations under the License.

#ifndef YOLO_H
#define YOLO_H

#include <opencv2/core/core.hpp>

#include <net.h>
typedef enum {
    POWER_ALL = 0,
    POWER_BIG = 1,
    POWER_LITTLE = 2
} PowerMode;

struct Object
{
    cv::Rect_<float> rect;
    int label;
    float prob;
};
struct GridAndStride
{
    int grid0;
    int grid1;
    int stride;
};
class Yolo
{
public:
    Yolo();

    char* input_name="images";
    char* output_name="output0";
    int num_class = 80;

    int load(const char *protopath, const char *modelpath, int _target_size, const float *_mean_vals,
         const float *_norm_vals, bool use_gpu);

    int detect(const cv::Mat& rgb, std::vector<Object>& objects, float prob_threshold = 0.4f,
               float nms_threshold = 0.5f);


private:
    ncnn::Net yolo;
    int target_size;
    float mean_vals[3];
    float norm_vals[3];
    ncnn::UnlockedPoolAllocator blob_pool_allocator;
    ncnn::PoolAllocator workspace_pool_allocator;
};

#endif // NANODET_H
