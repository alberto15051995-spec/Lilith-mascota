package com.lilith.companion;

import android.content.Context;
import android.graphics.*;
import android.view.View;

public class MascotView extends View {
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private long reactUntil=0;
    public MascotView(Context c){super(c);}
    public void react(){reactUntil=android.os.SystemClock.uptimeMillis()+900;invalidate();}
    @Override protected void onDraw(Canvas real){
        super.onDraw(real);
        Canvas c=real; int save=c.save();
        c.translate(getWidth()/2f,getHeight()/2f);float s=Math.min(getWidth()/300f,getHeight()/300f);c.scale(s,s);
        float t=android.os.SystemClock.uptimeMillis()/700f;
        boolean happy=android.os.SystemClock.uptimeMillis()<reactUntil;
        c.translate(0,(float)Math.sin(t)*3);
        p.setStyle(Paint.Style.FILL);
        p.setColor(0xff691a34);
        Path left=new Path();left.moveTo(-35,-25);left.cubicTo(-130,-130,-160,0,-95,45);left.quadTo(-82,15,-35,40);left.close();c.drawPath(left,p);
        Path right=new Path();right.moveTo(35,-25);right.cubicTo(130,-130,160,0,95,45);right.quadTo(82,15,35,40);right.close();c.drawPath(right,p);
        p.setColor(0xff21101d);c.drawOval(-45,0,45,110,p);
        p.setColor(0xffddd0c8);c.drawOval(-56,-94,56,26,p);
        p.setColor(0xff21101d);
        Path horn1=new Path();horn1.moveTo(-34,-76);horn1.quadTo(-95,-140,-65,-155);horn1.quadTo(-78,-112,-17,-91);horn1.close();c.drawPath(horn1,p);
        Path horn2=new Path();horn2.moveTo(34,-76);horn2.quadTo(95,-140,65,-155);horn2.quadTo(78,-112,17,-91);horn2.close();c.drawPath(horn2,p);
        p.setColor(0xff7b1728);c.drawOval(-33,-44,-10,-30,p);c.drawOval(10,-44,33,-30,p);
        p.setColor(Color.WHITE);c.drawCircle(-19,-38,3,p);c.drawCircle(19,-38,3,p);
        p.setColor(0xff63213a);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(3);
        c.drawArc(-15,-25,15,-8,happy?5:15,happy?170:150,false,p);
        p.setStyle(Paint.Style.FILL);
        c.restoreToCount(save);
        postInvalidateDelayed(50);
    }
}
