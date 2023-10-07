// Compression And Decompression

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.Scanner;

public class VectorQuantization {

    public static int[][] originalImage;

    //read 2D int pixels from image file
    public static int[][] readImage(String filePath) {
        int width;
        int height;
        File file = new File(filePath);
        BufferedImage image = null;
        try {
            image = ImageIO.read(file);
        } catch (IOException e) {
            e.printStackTrace();
        }

        assert image != null;
        width = image.getWidth();
        height = image.getHeight();
        int[][] pixels = new int[height][width];

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int rgb = image.getRGB(x, y);
                int r = (rgb >> 16) & 0xff;

                pixels[y][x] = r;
            }
        }

        return pixels;
    }

    public static void writeImage(int[][] pixels, String outputFilePath, int width, int height) {
        File fileOut = new File(outputFilePath);
        BufferedImage image2 = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                image2.setRGB(x, y, (pixels[y][x] << 16) | (pixels[y][x] << 8) | (pixels[y][x]));
            }
        }
        try {
            ImageIO.write(image2, "jpg", fileOut);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static class vector {
        int width;
        int height;
        double[][] data;

        public vector(int width, int height) {
            this.width = width;
            this.height = height;
            this.data = new double[height][width];
        }

    }

    static class split_element {
        vector value;
        ArrayList<vector> associated = new ArrayList<>();

        public split_element() {
        }

        public split_element(vector value, ArrayList<vector> associated) {
            this.value = value;
            this.associated = associated;
        }

        public vector getValue() {
            return value;
        }

        public void setValue(vector value) {
            this.value = value;
        }

        public ArrayList<vector> getAssociated() {
            return associated;
        }

    }

    static ArrayList<vector> Build_vectors(int[][] originalImage, vector[][] vectors, int numOfRows, int numOfCols, int widthOfBlock, int heightOfBlock) {

        ArrayList<vector> AllVectors = new ArrayList<>();
        vector curVector;

        for (int i = 0; i < originalImage.length; i += heightOfBlock) {
            for (int j = 0; j < originalImage[0].length; j += widthOfBlock) {
                int x = i;
                int z = j;
                curVector = new vector(widthOfBlock, heightOfBlock);
                //System.out.println("length = " + curVector.data.length);

                for (int n = 0; n < heightOfBlock; n++) {
                    for (int m = 0; m < widthOfBlock; m++) {
                        curVector.data[n][m] = originalImage[x][z++];
                    }

                    x++;
                    z = j;
                }

                AllVectors.add(curVector);
            }
        }

        int index = 0;

        for (int i = 0; i < numOfRows; i++) {
            for (int j = 0; j < numOfCols; j++) {
                vectors[i][j] = AllVectors.get(index++);
            }
        }

        return AllVectors;
    }

    static int indexOF_min_distance(ArrayList<Double> distance_difference) {
        double min_diff = distance_difference.get(0);
        int index = 0;

        for (int i = 1; i < distance_difference.size(); i++) {
            if (distance_difference.get(i) < min_diff) {
                min_diff = distance_difference.get(i);
                index = i;
            }

        }

        return index;
    }

    static ArrayList<vector> associate(ArrayList<vector> split, ArrayList<vector> data) // associate ang return avg
    {

        ArrayList<split_element> Split = new ArrayList<>();
        ArrayList<vector> Averages = new ArrayList<>();
        int width = data.get(0).width;
        int height = data.get(0).height;

        // initialization
        for (VectorQuantization.vector vector : split) {
            split_element initial = new split_element();
            initial.setValue(vector);
            Split.add(initial);
        }

        // associate data
        for (vector cur : data) {
            ArrayList<Double> distance_difference = new ArrayList<>();

            for (VectorQuantization.vector vector : split) {
                double total_diff = 0;

                for (int w = 0; w < width; w++) {
                    for (int h = 0; h < height; h++) {
                        double value = cur.data[w][h] - vector.data[w][h];
                        double distance_diff = Math.pow(value, 2);
                        total_diff += distance_diff;
                    }
                }

                distance_difference.add(total_diff);

            }

            int index = indexOF_min_distance(distance_difference);

            ArrayList<vector> cur_associated = Split.get(index).getAssociated();

            cur_associated.add(cur);

            split_element New = new split_element(Split.get(index).getValue(), cur_associated);

            Split.set(index, New);

        }

        // calculate average for the associated values
        for (VectorQuantization.split_element split_element : Split) {
            int arraySize = split_element.getAssociated().size();
            vector avg = new vector(width, height);

            for (int w = 0; w < width; w++) {
                for (int h = 0; h < height; h++) {
                    double total = 0;
                    for (int j = 0; j < arraySize; j++) {
                        total += split_element.getAssociated().get(j).data[w][h];
                    }
                    avg.data[w][h] = total / arraySize;
                }
            }
            Averages.add(avg);
        }

        return Averages;
    }

    static ArrayList<vector> Split(ArrayList<vector> Averages, ArrayList<vector> data, int levels) // split original averages
    {
        int width = Averages.get(0).width;
        int height = Averages.get(0).height;

        for (int i = 0; i < Averages.size(); i++) {
            if (Averages.size() < levels) {
                ArrayList<vector> split = new ArrayList<>();
                for (vector average : Averages) {
                    vector left = new vector(width, height);
                    vector right = new vector(width, height);
                    for (int w = 0; w < width; w++) {
                        for (int h = 0; h < height; h++) {
                            int cast = (int) average.data[w][h];
                            left.data[w][h] = cast;
                            right.data[w][h] = cast + 1;
                        }

                    }
                    split.add(left);
                    split.add(right);
                }
                Averages.clear();
                Averages = associate(split, data);
                i = 0;

            } else
                break;

        }

        return Averages;
    }

    static ArrayList<vector> modify(ArrayList<vector> prev_Averages, ArrayList<vector> new_Averages, ArrayList<vector> data) {
        while (true) {
            int width = new_Averages.get(0).width;
            int height = new_Averages.get(0).height;
            int total_diff = 0;
            int avg_diff;

            for (int i = 0; i < new_Averages.size(); i++) {
                double DiffOf2vec = 0;

                for (int w = 0; w < width; w++) {
                    for (int h = 0; h < height; h++) {
                        DiffOf2vec += Math.abs(prev_Averages.get(i).data[w][h] - new_Averages.get(i).data[w][h]);
                    }
                }

                total_diff += DiffOf2vec;
            }

            avg_diff = total_diff / prev_Averages.size();

            if (avg_diff < 0.0001) {
                break;
            } else {
                prev_Averages = new_Averages;
                new_Averages = associate(new_Averages, data);
            }

        }

        return new_Averages;

    }

    static void Quantization(int levels, ArrayList<vector> data, int widthOfBlock, int heightOfBlock, vector[][] vectors, int numOfCols) {
        ArrayList<vector> Averages = new ArrayList<>();
        // initialize first avg
        vector first_avg = new vector(widthOfBlock, heightOfBlock);

        for (int w = 0; w < widthOfBlock; w++) {
            for (int h = 0; h < heightOfBlock; h++) {
                double total = 0;

                for (vector datum : data) {
                    total += datum.data[w][h];

                }

                first_avg.data[w][h] = total / data.size();

            }

        }

        Averages.add(first_avg);

        Averages = Split(Averages, data, levels);
        System.out.println("Done split ");


        ArrayList<vector> prev_Averages = Averages;
        ArrayList<vector> new_Averages = associate(Averages, data);

        new_Averages = modify(prev_Averages, new_Averages, data);


        ArrayList<vector> codeBook = new ArrayList<>(new_Averages);


        int index = 0;


        for (int i = 0; i < widthOfBlock; i++) {
            for (int j = 0; j < numOfCols; j++) {
                vectors[i][j] = data.get(index++);
            }
        }

        compress(codeBook, vectors);

    }

    static void compress(ArrayList<vector> codeBook, vector[][] vectors) {
        int Rows = vectors.length;
        int Cols = vectors[0].length;
        int[][] comp_image = new int[Rows][Cols];

        for (int i = 0; i < Rows; i++) {
            for (int j = 0; j < Cols; j++) {
                vector cur = vectors[i][j];
                ArrayList<Double> distance_difference = new ArrayList<>();

                for (VectorQuantization.vector vector : codeBook) {
                    double total_diff = 0;

                    for (int w = 0; w < codeBook.get(0).width; w++) {
                        for (int h = 0; h < codeBook.get(0).height; h++) {
                            double value = cur.data[w][h] - vector.data[w][h];
                            double distinct_diff = Math.pow(value, 2);
                            total_diff += distinct_diff;
                        }
                    }

                    distance_difference.add(total_diff);
                }

                int index = indexOF_min_distance(distance_difference);
                comp_image[i][j] = index;

            }
        }


        Save_CodeBook_CompImg(codeBook, comp_image);

    }

    static Scanner sc;

    public static void open_file(String FileName) {
        try {
            sc = new Scanner(new File(FileName));
        } catch (Exception ignored) {

        }
    }

    public static void close_file() {
        sc.close();
    }

    static Formatter out;

    public static void openFile(String pass) {
        try {
            out = new Formatter(pass);
        } catch (Exception ignored) {
        }

    }

    public static void closeFile() {
        out.close();
    }

    static void write(String code) {
        out.format("%s", code);
        out.format("%n");
        out.flush();
    }


    static void Decompress() {

        ArrayList<vector> codeBook = new ArrayList<>();
        int[][] comp_image;
        comp_image = Reconstruct(codeBook);
        int[][] Decompressed_image = new int[originalImage.length][originalImage[0].length];

        for (int i = 0; i < comp_image.length; i++) {
            for (int j = 0; j < comp_image[0].length; j++) {
                vector cur;
                cur = codeBook.get(comp_image[i][j]);

                int cornerX = i * cur.height;
                int cornerY = j * cur.width;


                for (int h = 0; h < cur.height; h++) {

                    for (int k = 0; k < cur.width; k++) {
                        Decompressed_image[cornerX + h][cornerY + k] = (int) cur.data[h][k];
                    }
                }

            }
        }
        writeImage(Decompressed_image, "imgs/Decompress.jpg", Decompressed_image[0].length, Decompressed_image.length);

    }

    static void Save_CodeBook_CompImg(ArrayList<vector> codeBook, int[][] comp_image) {
        openFile("CompressFile.txt");
        String codeBookSize = "" + codeBook.size();
        String WidthOfBlock = "" + codeBook.get(0).width;
        String heightOfBlock = "" + codeBook.get(0).height;

        write(codeBookSize);
        write(WidthOfBlock);
        write(heightOfBlock);

        for (VectorQuantization.vector vector : codeBook) {
            for (int w = 0; w < vector.width; w++) {
                StringBuilder row = new StringBuilder();

                for (int h = 0; h < vector.height; h++) {
                    row.append(vector.data[w][h]).append(" ");
                }

                write(row.toString());
            }

        }

        String com_image_height = "" + comp_image.length;
        write(com_image_height);
        String com_image_width = "" + comp_image[0].length;
        write(com_image_width);

        for (int[] ints : comp_image) {
            StringBuilder row = new StringBuilder();
            for (int j = 0; j < comp_image[0].length; j++) {
                row.append(ints[j]).append(" ");
            }

            write(row.toString());
        }
        closeFile();
    }


    static int[][] Reconstruct(ArrayList<vector> codeBook) {
        open_file("CompressFile.txt");
        int codeBookSize = Integer.parseInt(sc.nextLine());
        int WidthOfBlock = Integer.parseInt(sc.nextLine());
        int heightOfBlock = Integer.parseInt(sc.nextLine());

        for (int i = 0; i < codeBookSize; i++) {
            vector cur = new vector(WidthOfBlock, heightOfBlock);

            for (int w = 0; w < WidthOfBlock; w++) {
                String row = sc.nextLine();
                String[] elements = row.split(" ");

                for (int h = 0; h < heightOfBlock; h++) {
                    cur.data[w][h] = Double.parseDouble(elements[h]);
                }

            }

            codeBook.add(cur);

        }  // done reading codeBook

        int com_image_height = Integer.parseInt(sc.nextLine());
        int com_image_width = Integer.parseInt(sc.nextLine());
        int[][] comp_image = new int[com_image_height][com_image_width];

        for (int i = 0; i < comp_image.length; i++) {
            String line = sc.nextLine();
            String[] row = line.split(" ");

            for (int j = 0; j < comp_image[0].length; j++) {
                comp_image[i][j] = Integer.parseInt(row[j]);
            }

        }

        close_file();

        return comp_image;

    }


    public static void main(String[] args) {
        Scanner myObj = new Scanner(System.in);
        System.out.println("Enter Number of levels: ");    //8
        int levels = myObj.nextInt();
        System.out.println("Enter Width: ");               //4
        int width = myObj.nextInt();
        System.out.println("Enter Height: ");              //4
        int height = myObj.nextInt();
        originalImage = readImage("imgs/D600x600.jpg");

        int numOfRows = originalImage.length / height;
        int numOfCols = originalImage[0].length / height;
        vector[][] vectors = new vector[numOfRows][numOfCols];
        ArrayList<vector> data = Build_vectors(originalImage, vectors, numOfRows, numOfCols, width, height);
        Quantization(levels, data, width, height, vectors, numOfCols);

        Decompress();

    }
}
