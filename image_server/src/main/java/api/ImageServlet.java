package api;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dao.Image;
import dao.ImageDao;
import org.apache.commons.codec.cli.Digest;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class ImageServlet extends HttpServlet {
    /**
     * 查看图片属性：既能查看所有，又能查看指定
     *
     * @param req
     * @param resp
     * @throws ServletException
     * @throws IOException
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        //考虑到查看所有图片属性和查看指定图片属性
        //通过url中是否带有imageId参数来进行区分
        //存在imageId ->查看指定图片属性，否则查看所有图片属性
        //例如：URL /image?imageId=100,imageId的值就是100
        //如果URL中不存在imageId的话那么就返回一个null
        String imageId = req.getParameter("imageId");
        if (imageId == null || imageId.equals("")) {
            //查看所有图片属性
            selectAll(req, resp);
        } else {
            //查看指定图片属性
            selectOne(imageId, resp);
        }
    }

    private void selectAll(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=utf-8");
        //1.创建一个ImageDao对象，并查找数据库
        ImageDao imageDao = new ImageDao();
        List<Image> images = imageDao.selectAll();
        //2.把查找到的结果转成JSON格式的字符串，并写回给resp对象
        Gson gson = new GsonBuilder().create();
        //jsonData就是一个json格式的字符串了，就和之前约定的格式是一样的了
        //重点体会下面这行代码，这个方法的核心所在就是gson帮我们完成了大量的格式转换
        //只要把之前的相关字段都约定成统一的命名，下面的操作就可以一步到位的完成整个转换
        String jsonData = gson.toJson(images);
        resp.getWriter().write(jsonData);
    }

    private void selectOne(String imageId, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=utf-8");
        //1.创建ImageDao对象
        ImageDao imageDao = new ImageDao();
        Image image = imageDao.selectOne(Integer.parseInt(imageId));
        //2.使用gson把查到的数据转换成json格式，并写回给响应
        Gson gson = new GsonBuilder().create();
        String jsonData = gson.toJson(image);
        resp.getWriter().write(jsonData);
    }

    /**
     * 上传图片
     *
     * @param req
     * @param resp
     * @throws ServletException
     * @throws IOException
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        //1.获取图片属性信息，并存入数据库
        //a)需要一个factory对象和upload对象,这是为了获取图片属性所作的准备工作——固定的逻辑
        FileItemFactory factory = new DiskFileItemFactory();
        ServletFileUpload upload = new ServletFileUpload(factory);
        List<FileItem> items = null;
        //b)通过upload对象进一步解析请求(解析HTTP请求中奇怪的body中的内容)
        //FileItem就代表一个上传的对象，理论上来说，HTTP支持一个请求中上传多个文件
        try {
            items = upload.parseRequest(req);

        } catch (FileUploadException e) {
            //如果捕获到异常说明解析出错了!
            e.printStackTrace();
            //告诉客户端出现的具体错误是啥
            resp.setContentType("application/json;charset=utf-8");
            resp.getWriter().write("{\"ok\":false,\"reason\":\"请求解析失败\"}");//json格式
            return;
        }
        //c)FileItem中的属性提取出来，转换成Image对象，才能存到数据库中
        //当前只考虑一张图片的情况
        FileItem fileItem = items.get(0);
        Image image = new Image();
        image.setImageName(fileItem.getName());
        image.setSize((int) fileItem.getSize());
        //手动获取一下当前日期并转成一个格式化日期,yyMMdd->20210221
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMdd");
        image.setUploadTime(simpleDateFormat.format(new Date()));
        image.setContentType(fileItem.getContentType());
        image.setMd5(DigestUtils.md5Hex(fileItem.get()));
        //构造一个路径来保存图片,引入时间戳是为了让文件路径唯一，这是一种比较简单的方式
        image.setPath("./image/" + image.getMd5());
        //存到数据库中
        ImageDao imageDao = new ImageDao();

        //看看数据库中是否存在相同Md5值的图片，不存在，返回null
        Image existImage = imageDao.selectByMd5(image.getMd5());

        imageDao.insert(image);


        //2.获取图片内容信息，并存入磁盘文件
        if (existImage==null){
            File file = new File(image.getPath());
            try {
                fileItem.write(file);
            } catch (Exception e) {
                e.printStackTrace();
                resp.setContentType("application/json;charset=utf-8");
                resp.getWriter().write("{\"ok\":false,\"reason\":\"文件写入失败\"}");//json格式
                return;
            }
        }
        //3.给客户端返回一个结果
//        resp.setContentType("application/json;charset=utf-8");
//        resp.getWriter().write("{\"ok\":true}");
        resp.sendRedirect("index.html");
    }

    /**
     * 删除指定图片
     *
     * @param req
     * @param resp
     * @throws ServletException
     * @throws IOException
     */
    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json;charset=utf-8");
        //1.获取到请求中的imageId
        String imageId = req.getParameter("imageId");
        if (imageId == null || imageId.equals("")) {
            resp.setStatus(200);
            resp.getWriter().write("{\"ok\":false,\"reason\":解析请求失败}");
            return;
        }
        //2.创建ImageDao对象，查看到该图片对象的相关属性(这是为了知道这个图片对应的文件路径)
        ImageDao imageDao = new ImageDao();
        Image image = imageDao.selectOne(Integer.parseInt(imageId));
        if (image == null) {
            //请求中传入的id在数据库中不存在
            resp.setStatus(200);
            resp.getWriter().write("{\"ok\":false,\"reason\":imageId在数据库中不存在}");
            return;
        }
        //3.删除数据库中的记录
        imageDao.delete(Integer.parseInt(imageId));
        //4.删除本地磁盘文件
        File file = new File(image.getPath());
        file.delete();
        resp.setStatus(200);
        resp.getWriter().write("{\"ok\":true}");
    }
}
