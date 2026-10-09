package it.nmsitalia.corvettehub.ui;

import java.awt.*;

/** Righe che vanno a capo e dichiarano l'altezza realmente necessaria. */
public final class LayoutFluido implements LayoutManager2 {
    private final int gapX, gapY;
    public LayoutFluido(int gapX, int gapY) { this.gapX=Math.max(0,gapX); this.gapY=Math.max(0,gapY); }
    public void addLayoutComponent(Component c,Object constraints) { }
    public void addLayoutComponent(String name,Component c) { }
    public void removeLayoutComponent(Component c) { }
    public void invalidateLayout(Container c) { }
    public float getLayoutAlignmentX(Container c) { return 0; }
    public float getLayoutAlignmentY(Container c) { return 0; }
    public Dimension maximumLayoutSize(Container c) { return new Dimension(Integer.MAX_VALUE,preferredLayoutSize(c).height); }
    public Dimension minimumLayoutSize(Container c) { return new Dimension(0,preferredLayoutSize(c).height); }
    public Dimension preferredLayoutSize(Container c) { return misura(c,false); }
    public void layoutContainer(Container c) { misura(c,true); }
    private Dimension misura(Container c,boolean layout) {
        Insets in=c.getInsets(); int width=c.getWidth();
        if(!layout && c.getParent()!=null && c.getParent().getLayout() instanceof BorderLayout) {
            BorderLayout parentLayout=(BorderLayout)c.getParent().getLayout();
            Object position=parentLayout.getConstraints(c);
            if(BorderLayout.NORTH.equals(position)||BorderLayout.SOUTH.equals(position)||BorderLayout.CENTER.equals(position)) {
                Insets parentInsets=c.getParent().getInsets();
                width=c.getParent().getWidth()-parentInsets.left-parentInsets.right;
            }
        }
        if(width<=0 && c.getParent()!=null) width=c.getParent().getWidth();
        if(width<=0) {
            width=in.left+in.right;
            for(Component child:c.getComponents()) if(child.isVisible()) width+=child.getPreferredSize().width+gapX;
        }
        int available=Math.max(1,width-in.left-in.right),x=0,y=0,row=0,used=0;
        for(Component child:c.getComponents()) {
            if(!child.isVisible()) continue;
            Dimension d=child.getPreferredSize(); int w=Math.min(available,d.width),h=d.height;
            if(x>0 && x+w>available) { y+=row+gapY; x=0; row=0; }
            if(layout) child.setBounds(in.left+x,in.top+y,w,h);
            used=Math.max(used,x+w);row=Math.max(row,h);x+=w+gapX;
        }
        return new Dimension(used+in.left+in.right,y+row+in.top+in.bottom);
    }
}
