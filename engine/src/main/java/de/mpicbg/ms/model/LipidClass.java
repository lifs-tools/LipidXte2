package de.mpicbg.ms.model;

/**
 * LipidClass data structure
 */
public class LipidClass
{
   final private String name;
   final private AliphaticMoiety aliphaticMoiety;
   final private int faAnions;
   final private boolean sn2;
   final private boolean sym;
   final private boolean sn1;

   public LipidClass( String name, AliphaticMoiety aliphaticMoiety, int faAnions, int sn2, int sym, int sn1 )
   {
      this.name = name;
      this.aliphaticMoiety = aliphaticMoiety;
      this.faAnions = faAnions;
      this.sn2 = sn2 == 1;
      this.sym = sym == 1;
      this.sn1 = sn1 == 1;
   }

   @Override public String toString()
   {
      return this.name;
   }

   public boolean isSn2()
   {
      return sn2;
   }

   public boolean isSym()
   {
      return sym;
   }

   public boolean isSn1()
   {
      return sn1;
   }
}
