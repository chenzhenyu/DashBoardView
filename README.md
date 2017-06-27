# DashBoardView
公司项目，赶时间刚出的一个效果，很多参数没有可配置，有需要可以看看DashboardView的常量字段，添加公开接口设值就可以了。效果如下：

![image](https://github.com/chenzhenyu/DashBoardView/blob/master/img/dashboard.png)

Demo：
//初始化 <br>
dvSpeed.setDIVIDER_GROUP(16);//大刻度有16个跨度<br>
dvSpeed.setDIVIDER_CHILD(2);//每个大刻度下有两个小刻度<br>
dvSpeed.setUNIT_MULTIPLE(10);//每个大刻度的数值是大刻度的index乘以这里设置的值<br>
dvSpeed.setWHERE_HIGH(10);//哪里用红色标记为高速<br>

//设置数据<br>
dvSpeed.smoothLoadValue(speed);<br>
