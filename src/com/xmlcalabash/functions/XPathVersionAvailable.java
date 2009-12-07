package com.xmlcalabash.functions;

import net.sf.saxon.functions.ExtensionFunctionDefinition;
import net.sf.saxon.functions.ExtensionFunctionCall;
import net.sf.saxon.expr.*;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.StringValue;
import net.sf.saxon.value.SequenceType;
import net.sf.saxon.value.UntypedAtomicValue;
import net.sf.saxon.value.BooleanValue;
import net.sf.saxon.value.DoubleValue;
import net.sf.saxon.om.*;
import com.xmlcalabash.core.XProcConstants;
import com.xmlcalabash.core.XProcRuntime;

//
// The contents of this file are subject to the Mozilla Public License Version 1.0 (the "License");
// you may not use this file except in compliance with the License. You may obtain a copy of the
// License at http://www.mozilla.org/MPL/
//
// Software distributed under the License is distributed on an "AS IS" basis,
// WITHOUT WARRANTY OF ANY KIND, either express or implied.
// See the License for the specific language governing rights and limitations under the License.
//
// The Original Code is: all this file.
//
// The Initial Developer of the Original Code is Michael H. Kay.
//
// Portions created by Norman Walsh are Copyright (C) Mark Logic Corporation. All Rights Reserved.
//
// Contributor(s): Norman Walsh.
//

/**
 * Implementation of the XSLT system-property() function
 */

public class XPathVersionAvailable extends ExtensionFunctionDefinition {
    private static StructuredQName funcname = new StructuredQName("p", XProcConstants.NS_XPROC, "xpath-version-available");
    private XProcRuntime runtime = null;

     protected XPathVersionAvailable() {
         // you can't call this one
     }

     public XPathVersionAvailable(XProcRuntime runtime) {
         this.runtime = runtime;
     }

     public StructuredQName getFunctionQName() {
         return funcname;
     }

     public int getMinimumNumberOfArguments() {
         return 1;
     }

     public int getMaximumNumberOfArguments() {
         return 1;
     }

     public SequenceType[] getArgumentTypes() {
         return new SequenceType[]{SequenceType.SINGLE_DOUBLE};
     }

     public SequenceType getResultType(SequenceType[] suppliedArgumentTypes) {
         return SequenceType.SINGLE_BOOLEAN;
     }

     public ExtensionFunctionCall makeCallExpression() {
         return new SystemPropertyCall();
     }

     private class SystemPropertyCall extends ExtensionFunctionCall {
         public SequenceIterator call(SequenceIterator[] arguments, XPathContext context) throws XPathException {
             SequenceIterator iter = arguments[0];
             DoubleValue value = (DoubleValue) iter.next();
             double reqVer = value.getDoubleValue();

             return SingletonIterator.makeIterator((reqVer == 1.0 || reqVer == 2.0) ? BooleanValue.TRUE : BooleanValue.FALSE);
         }
     }
}