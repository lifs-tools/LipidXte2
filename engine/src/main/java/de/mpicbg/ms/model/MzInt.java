package de.mpicbg.ms.model;

/**
 * Mass and Intensity class
 */
public class MzInt
{
   Double mz;
   Float intensity;

   public MzInt( double mz, float intensity )
   {
      this.mz = mz;
      this.intensity = intensity;
   }

   public Double getMz()
   {
      return mz;
   }

   public void setMz( Double mz )
   {
      this.mz = mz;
   }

   public Float getIntensity()
   {
      return intensity;
   }

   public void setIntensity( Float intensity )
   {
      this.intensity = intensity;
   }
}
