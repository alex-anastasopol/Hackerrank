package ro.cst.tsearch.generic;


public class StaticPrintFormat {
    private static char p = ',';
    private static char q = '.';
    private static StringBuffer n = new StringBuffer();
    private static int o = 3;
    
    public static String sprintf(String s1, Object aobj[]) {
        StringBuffer stringbuffer = new StringBuffer("");
        StringBuffer stringbuffer1 = new StringBuffer("");
        int i1 = 0;
        int j1 = s1.length();
        int k1 = 0;
        
        while(k1 < j1)  {
            if(s1.charAt(k1) != '%' || aobj == null || i1 >= aobj.length) {
                stringbuffer.append(s1.charAt(k1));
                k1++;
                continue;
            }
            if(k1 < j1 - 1 && s1.charAt(k1 + 1) == '%') {
                stringbuffer.append('%');
                k1 += 2;
                continue;
            }
            int l1 = 0;
            int i2 = -1;
            boolean flag = false;
            boolean flag1 = false;
            int j2 = 1;
            boolean flag2 = false;
            int k2;
            
            for(k2 = k1 + 1; k2 < j1 && j2 != 5;) {
                char c1 = s1.charAt(k2);
                switch(j2) {
                case 5: // '\005'
                default:
                    break;

                case 1: // '\001'
                    if(c1 == '-') {
                        flag = true;
                        k2++;
                    }
                    j2 = 6;
                    break;

                case 6: // '\006'
                    if(c1 == p) {
                        flag2 = true;
                        k2++;
                    }
                    j2 = 2;
                    break;

                case 2: // '\002'
                    if(c1 == '0') {
                        flag1 = true;
                        k2++;
                    }
                    j2 = 3;
                    break;

                case 3: // '\003'
                    if(Character.isDigit(c1)) {
                        l1 = 10 * l1 + Character.digit(c1, 10);
                        k2++;
                        break;
                    }
                    if(c1 == q) {
                        j2 = 4;
                        k2++;
                    } else {
                        j2 = 4;
                    }
                    break;

                case 4: // '\004'
                    if(Character.isDigit(c1)) {
                        if(i2 < 0)
                            i2 = 0;
                        i2 = 10 * i2 + Character.digit(c1, 10);
                        k2++;
                    } else {
                        j2 = 5;
                    }
                    break;
                }
            }

            if(j2 != 5 || k2 == j1) {
                stringbuffer.append('%');
                k1++;
                continue;
            }
            stringbuffer1.setLength(0);
            switch(s1.charAt(k2)) {
            case 115: // 's'
                stringbuffer1.append(aobj[i1]);
                i1++;
                k1 = k2 + 1;
                if(flag)
                    b(stringbuffer1, l1, ' ');
                else
                    a(stringbuffer1, l1, ' ');
                if(i2 > -1 && stringbuffer1.length() > i2)
                    stringbuffer1.setLength(i2);
                break;

            case 100: // 'd'
                if(!(aobj[i1] instanceof Number)) {
                    stringbuffer.append('%');
                    k1++;
                } else {
                    Number number = (Number)aobj[i1];
                    if(!flag1 && flag2)
                        stringbuffer1.append(b(String.valueOf(number.longValue())));
                    else
                        stringbuffer1.append(number.longValue());
                    i1++;
                    k1 = k2 + 1;
                    if(flag1) {
                        a(stringbuffer1, l1, '0');
                        break;
                    }
                    if(flag)
                        b(stringbuffer1, l1, ' ');
                    else
                        a(stringbuffer1, l1, ' ');
                    break;
                }
                continue;

            case 120: // 'x'
                if(!(aobj[i1] instanceof Number)) {
                    stringbuffer.append('%');
                    k1++;
                } else {
                    Number number1 = (Number)aobj[i1];
                    String s2 = Long.toString(number1.longValue(), 16);
                    if(!flag1 && flag2)
                        stringbuffer1.append(b(s2));
                    else
                        stringbuffer1.append(s2);
                    i1++;
                    k1 = k2 + 1;
                    if(flag1) {
                        a(stringbuffer1, l1, '0');
                        break;
                    }
                    if(flag)
                        b(stringbuffer1, l1, ' ');
                    else
                        a(stringbuffer1, l1, ' ');
                    break;
                }
                continue;

            case 103: // 'g'
                if(!(aobj[i1] instanceof Number)) {
                    stringbuffer.append('%');
                    k1++;
                } else {
                    Number number2 = (Number)aobj[i1];
                    String s3 = number2.toString();
                    if(s3.endsWith(".0"))
                        s3 = s3.substring(0, s3.length() - 2);
                    if(q != '.')
                        s3 = s3.replace('.', q);
                    if(!flag1 && flag2)
                        stringbuffer1.append(b(s3));
                    else
                        stringbuffer1.append(s3);
                    i1++;
                    k1 = k2 + 1;
                    if(flag1) {
                        a(stringbuffer1, l1, '0');
                        break;
                    }
                    if(flag)
                        b(stringbuffer1, l1, ' ');
                    else
                        a(stringbuffer1, l1, ' ');
                    break;
                }
                continue;

            case 102: // 'f'
                if(!(aobj[i1] instanceof Number)) {
                    stringbuffer.append('%');
                    k1++;
                } else {
                    Number number3 = (Number)aobj[i1];
                    if(i2 < 0)
                        i2 = 2;
                    double d1 = number3.doubleValue();
                    if(i2 == 0) {
                        long l2 = (long)(d1 + 0.5D);
                        if(!flag1 && flag2)
                            stringbuffer1.append(b(String.valueOf(l2)));
                        else
                            stringbuffer1.append(l2);
                    } else {
                        boolean flag3 = false;
                        if(d1 < 0.0D) {
                            d1 = -d1;
                            flag3 = true;
                        }
                        long l3 = (long)d1;
                        d1 -= l3;
                        double d2 = 1.0D;
                        for(int i3 = 0; i3 < i2; i3++)
                            d2 *= 10D;

                        d1 = Math.floor(d1 * d2 + 0.5D);
                        if(d1 >= d2) {
                            d1 -= d2;
                            l3++;
                        }
                        stringbuffer1.append((long)d1);
                        a(stringbuffer1, i2, '0');
                        stringbuffer1.insert(0, q);
                        if(!flag1 && flag2)
                            stringbuffer1.insert(0, b(String.valueOf(l3)));
                        else
                            stringbuffer1.insert(0, l3);
                        if(flag3)
                            stringbuffer1.insert(0, '-');
                    }
                    i1++;
                    k1 = k2 + 1;
                    if(flag1) {
                        a(stringbuffer1, l1, '0');
                        break;
                    }
                    if(flag)
                        b(stringbuffer1, l1, ' ');
                    else
                        a(stringbuffer1, l1, ' ');
                    break;
                }
                continue;

            default:
                stringbuffer.append('%');
                k1++;
                continue;
            }
            stringbuffer.append(stringbuffer1);
        }
        return stringbuffer.toString();
    }


    private static String b(String s1) {
        String s2 = null;
        if(s1.indexOf("e+") != -1 || s1.indexOf("e-") != -1 || s1.indexOf("E+") != -1 || s1.indexOf("E-") != -1)
            return s1;
        synchronized(n) {
            n.setLength(0);
            int i1 = s1.length();
            int j1 = s1.indexOf(q);
            int k1 = i1 - 1;
            if(j1 != -1)
                for(; k1 >= j1; k1--)
                    n.insert(0, s1.charAt(k1));

            int l1 = 0;
            while(k1 >= 0)  {
                int i2 = s1.charAt(k1);
                if(i2 == 45) {
                    l1 = 0;
                    n.insert(0, '-');
                    k1--;
                } else {
                    if(l1 == o) {
                        n.insert(0, p);
                        l1 = 0;
                    }
                    n.insert(0, (char)i2);
                    l1++;
                    k1--;
                }
            }
            s2 = n.toString();
        }
        return s2;
    }

    private static void a(StringBuffer stringbuffer, int i1, char c1) {
        if(stringbuffer.length() < 1)
            return;
        for(int j1 = stringbuffer.length(); j1 < i1; j1++)
            if(stringbuffer.charAt(0) == '-' && c1 == '0')
                stringbuffer.insert(1, c1);
            else
                stringbuffer.insert(0, c1);

    }

    private static void b(StringBuffer stringbuffer, int i1, char c1) {
        for(int j1 = stringbuffer.length(); j1 < i1; j1++)
            stringbuffer.append(c1);

    }
}
