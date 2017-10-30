package tech.dronescan.util;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.log4j.Logger;

/**
 * This class can be used to automatically set the fields on anything passed to 'load'
 * from the values in a properties file. The properties file loaded must contain values 
 * that correspond to the field names in the child class.
 * 
 * This will do automatic conversions for built in types or their equivalents. It will also 
 * handle a comma separated list as an array.
 * 
 * The resource name provided to the load method will be found by first searching for a properties 
 * file using that name as a filename and secondly as a resource in the classpath. In this way you 
 * can use defaults bundled with an application yet override them by including the properties file
 * along a path.
 * 
 * @author jim
 *
 */
public class ConfigurationUtils 
{
   static private Logger logger = Logger.getLogger(ConfigurationUtils.class);
   
   static private File confDir;
   static private final String configDirName = ".knighttrader";
   static private final String configFileName = "conf.properties";
   
   static 
   {
      restoreConfDirectory();
   }
   
   /**
    * This method is for testing purposes only
    */
   public static void overrideConfDirectory(String newHomeDir) { confDir = new File(newHomeDir);  }
   
   /**
    * This method is public for testing purposes only
    */
   public static void restoreConfDirectory() 
   {
      String homeDirPath = System.getProperty("user.home");
      File tmpDir = new File(homeDirPath);
      confDir = new File(tmpDir,configDirName);
      if (!confDir.exists())
         confDir.mkdirs();
   }

   public static File getConfigDirectory() { return confDir; }
   
   public static File getConfigFile() { return new File(confDir,configFileName); }
   
   public static void configure(Object obj) throws IOException
   {
      File conf = getConfigFile();
      if (conf.exists())
         load(obj,conf.getAbsolutePath());
   }
   
   public static void writeAsProperties(Object obj, PrintStream out)
   {
      Class<?> configClass = obj.getClass();
      Field[] fields = configClass.getFields();
      for (Field field : fields)
      {
         Object val;
         try
         {
            val = field.get(obj);
         }
         catch (IllegalAccessException iae)
         {
            throw new RuntimeException("Unlikely IllegalAccessException.", iae);
         }

         if (val.getClass().isArray())
         {
            int len = Array.getLength(val);
            if (len > 0)
            {
               StringBuilder sb = new StringBuilder(Array.get(val, 0).toString());
               for (int i = 1; i < len; i++)
               {
                  sb.append(',');
                  sb.append(Array.get(val, i).toString());
               }


               val = sb.toString();
            }
            else
               val = "";
         }

         out.println(field.getName() + "=" + val);
      }
   }

   public static void load(Object configObj, String configurationResource) throws IOException
   {
      try
      {
         Class<?> configClass = configObj.getClass();
         Field[] fields = configClass.getFields();

         Map<String,Field> fieldMap = new HashMap<String,Field>();
         for (Field field : fields)
            fieldMap.put(field.getName(),field);

         // If this throws an IOException then there was no file
         //   or resource available.
         Config config = null;

         // check to see if a property was set to point to the properties file
         String tmps = System.getProperty("config");
         if (tmps == null)
            tmps = configurationResource;

         try
         {
            config = new Config(tmps);
         }
         catch (FileNotFoundException fnfe)
         {
            config = null;
         }

         if (config == null)
         {
            if (logger.isDebugEnabled())
               logger.debug("Confiuration not loaded from properties. All defaults being used.");
         }
         else
         {

            // now we need to go through one entry at a time and match up
            //  the fields with the prop file entries.
            Set<Object> propKeys = config.keySet();
            for (Object propKey : propKeys)
            {
               // first look up the field.
               Field field = fieldMap.get(propKey);

               if (field != null)
               {
                  String value = (String)config.get(propKey);

                  if (logger.isDebugEnabled())
                     logger.debug(configClass.getSimpleName() + " entry \"" + propKey + " \" is overriden from property file with " + value);

                  // now consider the type of the field and set it from 
                  // the string.

                  // all fields must also be either native, an array
                  //  of natives, or a string.
                  Class<?> fieldType = field.getType();
                  if (fieldType.isArray())
                  {
                     Class<?> componentType = fieldType.getComponentType();

                     // we need to build an array from the properties. Assume comma separated.
                     List<Object> componentList = new ArrayList<Object>();
                     for (StringTokenizer stok = new StringTokenizer(value,",");stok.hasMoreTokens();)
                     {
                        String cur = stok.nextToken();
                        componentList.add(convertToType(componentType,cur));
                     }
                     field.set(configObj, convertArray(componentType,componentList));
                  }
                  else
                  {
                     Object objValue = convertToType(fieldType, value);
                     if (objValue != null)
                        field.set(configObj, objValue);
                     else if (logger.isDebugEnabled())
                        logger.debug("Cannot set object of type " + fieldType.getName() );
                  }
               }
               else
               {
                  if (logger.isDebugEnabled())
                     logger.debug("WARNING: The property \"" + propKey + "\" has no coresponding field on " + configObj.getClass().getSimpleName() +  ".");
               }
            } // end loop over the properties in the properties file.
         }// end the else for whether or not there's overriden properties.
      }
      catch (IllegalAccessException iae)
      {
         // this can't really happen because getFields returns public fields
         throw new RuntimeException("Unlikely IllegalAccessException.", iae);
      }

   }

   private static Object convertToType(Class<?> convertTo, String value)
   {
      Object ret = null;

      if ("java.lang.String".equals(convertTo.getName()))
         ret = value;
      else if (convertTo == boolean.class || convertTo == Boolean.class)
         ret = new Boolean("true".equalsIgnoreCase(value) || "t".equalsIgnoreCase(value) ||
               "yes".equalsIgnoreCase(value) || "y".equalsIgnoreCase(value) ||
               "1".equalsIgnoreCase(value));
      else if (convertTo == long.class || convertTo == Long.class)
         ret = new Long(Long.parseLong(value));
      else if (convertTo == short.class || convertTo == Short.class)
         ret = new Short(Short.parseShort(value));
      else if (convertTo == int.class || convertTo == Integer.class)
         ret = new Integer(Integer.parseInt(value));
      else if (convertTo == double.class || convertTo == Double.class)
         ret = new Double(Double.parseDouble(value));
      else if (convertTo == float.class || convertTo == Float.class)
         ret = new Float(Float.parseFloat(value));
      else if (convertTo == char.class || convertTo == Character.class)
         ret = new Character(value.charAt(0));
      else if (convertTo == byte.class || convertTo == Byte.class)
         ret = new Byte(Byte.parseByte(value));

      return ret;
   }

   private static Object convertArray(Class<?> componentType, List<Object> list)
   {
      Object ret = Array.newInstance(componentType,list.size());
      for (int i = 0; i < list.size(); i++)
         Array.set(ret,i,list.get(i));
      return ret;
   }

   private static class Config extends Properties 
   {  
      private static final long serialVersionUID = 1L;
      private String resourcename = null;
      
      /**
       * Construct a Config object by searching for a properties file
       * first using it as a filename and secondly as a resource in the
       * classpath. In this way you can use defaults bundled with
       * an application yet override them by including the properties file
       * along a path.
       * 
       * @param resourceName
       * @throws IOException
       */

      public Config(String resourceName) throws IOException
      {
         this.resourcename = resourceName;
         reload();
      }
      
      public synchronized void reload() throws IOException
      {
         this.clear();
         
         InputStream istream = null;
         try
         {
            File propFile = new File(resourcename);
            if (propFile.exists() && propFile.isFile())
            {
               istream = new FileInputStream(propFile);
            }
            else
            {
               istream = getClass().getResourceAsStream(resourcename);
               
               if (istream == null)
                  istream = getClass().getResourceAsStream("/" + resourcename);
            }

            if (istream == null)
               throw new FileNotFoundException("Cannot find " + resourcename + 
                        " either as a file or on the classpath ");

            load(istream);
         }
         finally
         {
            if (istream != null)
               try { istream.close(); } catch (Throwable th) {}
         }
      }
   }

}
