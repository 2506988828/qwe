package com.my.qwe.util;

import com.my.qwe.task.TaskStepNotifier;
import com.my.qwe.task.TaskThread;

import java.util.ArrayList;
import java.util.List;

public class BagGridUtil {

    /**
     * 获取背包格子区域
     * @return 每个格子一个 int[4]，格式为 [x1, y1, x2, y2]
     */
    public static List<int[]> generateBagGrids() {
        List<int[]> grids = new ArrayList<>();

        // 假设一行为 5 个格子，共 4 行（20个格子）
        int startX = 345;
        int startY = 100;
        int cellWidth = 50;
        int cellHeight = 50;
        int columns = 5;
        int rows = 4;

        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < columns; col++) {
                int x1 = startX + col * cellWidth;
                int y1 = startY + row * cellHeight;
                int x2 = x1 + cellWidth;
                int y2 = y1 + cellHeight;
                grids.add(new int[]{x1, y1, x2, y2});
            }
        }

        return grids;
    }

    public static List<int[]> generateEmptyBagGrids() {
        List<int[]> grids = new ArrayList<>();

        // 假设一行为 5 个格子，共 4 行（20个格子）
        int startX = 363;
        int startY = 118;
        int cellWidth = 50;
        int cellHeight = 50;
        int columns = 5;
        int rows = 4;

        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < columns; col++) {
                int x1 = startX + col * cellWidth;
                int y1 = startY + row * cellHeight;
                int x2 = x1 + cellWidth;
                int y2 = y1 + cellHeight;
                grids.add(new int[]{x1, y1, x2, y2});
            }
        }

        return grids;
    }

    /**
     * 获取仓库格子区域
     * @return 每个格子一个 int[4]，格式为 [x1, y1, x2, y2]
     */
    public static List<int[]> generateCangkuGrids() {
        List<int[]> grids = new ArrayList<>();

        // 假设一行为 5 个格子，共 4 行（20个格子）
        int startX = 80;
        int startY = 120;
        int cellWidth = 50;
        int cellHeight = 50;
        int columns = 5;
        int rows = 4;

        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < columns; col++) {
                int x1 = startX + col * cellWidth;
                int y1 = startY + row * cellHeight;
                int x2 = x1 + cellWidth;
                int y2 = y1 + cellHeight;
                grids.add(new int[]{x1, y1, x2, y2});
            }
        }

        return grids;
    }

    public static List<int[]> jiyujiemianBagGrids() {
        List<int[]> grids = new ArrayList<>();

        // 假设一行为 5 个格子，共 4 行（20个格子）
        int startX = 453;
        int startY = 49;
        int cellWidth = 40;
        int cellHeight = 40;
        int hSpacing = 10; // 横向间距（像素）
        int vSpacing = 5;  // 纵向间距（像素）
        int columns = 5;
        int rows = 4;

        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < columns; col++) {
                // 计算每个格子的左上角坐标，考虑间距
                int x1 = startX + col * (cellWidth + hSpacing);
                int y1 = startY + row * (cellHeight + vSpacing);
                int x2 = x1 + cellWidth;
                int y2 = y1 + cellHeight;
                grids.add(new int[]{x1, y1, x2, y2});
            }
        }

        return grids;
    }

    public static List<int[]> cangkujiemianBagGrids() {
        List<int[]> grids = new ArrayList<>();

        // 假设一行为 5 个格子，共 4 行（20个格子）
        int startX = 360;
        int startY = 120;
        int cellWidth = 50;
        int cellHeight = 50;
        int columns = 5;
        int rows = 4;

        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < columns; col++) {
                int x1 = startX + col * cellWidth;
                int y1 = startY + row * cellHeight;
                int x2 = x1 + cellWidth;
                int y2 = y1 + cellHeight;
                grids.add(new int[]{x1, y1, x2, y2});
            }
        }

        return grids;
    }




    //获取道具所在格子的中心坐标
    public static int[] getGridCenter(int index) {
        List<int[]> grids = generateBagGrids();
        if (index < 0 || index >= grids.size()) {
            return new int[]{-1, -1};
        }

        int[] rect = grids.get(index); // rect = [x1, y1, x2, y2]
        int centerX = (rect[0] + rect[2]) / 2;
        int centerY = (rect[1] + rect[3]) / 2;
        return new int[]{centerX, centerY};
    }


}
