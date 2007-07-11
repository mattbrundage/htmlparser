/*
 * Copyright (c) 2007 Henri Sivonen
 * Portions of comments Copyright 2004-2007 Apple Computer, Inc., Mozilla 
 * Foundation, and Opera Software ASA.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a 
 * copy of this software and associated documentation files (the "Software"), 
 * to deal in the Software without restriction, including without limitation 
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, 
 * and/or sell copies of the Software, and to permit persons to whom the 
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in 
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR 
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, 
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL 
 * THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER 
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING 
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER 
 * DEALINGS IN THE SOFTWARE.
 */

/*
 * The comments following this one that use the same comment syntax as this 
 * comment are quotes from the WHATWG HTML 5 spec as of 27 June 2007 
 * amended as of June 28 2007.
 * That document came with this statement:
 * "© Copyright 2004-2007 Apple Computer, Inc., Mozilla Foundation, and 
 * Opera Software ASA. You are granted a license to use, reproduce and 
 * create derivative works of this document."
 */

package nu.validator.htmlparser;

import java.util.Arrays;

import org.xml.sax.Attributes;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

public abstract class TreeBuilder<T> implements TokenHandler {

    private enum Phase {
        INITIAL, ROOT_ELEMENT, BEFORE_HEAD, IN_HEAD, IN_HEAD_NOSCRIPT, AFTER_HEAD, IN_BODY, IN_TABLE, IN_CAPTION, IN_COLUMN_GROUP, IN_TABLE_BODY, IN_ROW, IN_CELL, IN_SELECT, AFTER_BODY, IN_FRAMESET, AFTER_FRAMESET, TRAILING_END
    }

    private class StackNode<S> {
        final String name;

        final S node;
        
        final boolean scoping;
        
        final boolean special;

        final boolean fosterParenting;
        
        /**
         * @param name
         * @param node
         * @param scoping
         * @param special
         */
        StackNode(final String name, final S node, final boolean scoping, final boolean special, final boolean fosterParenting) {
            this.name = name;
            this.node = node;
            this.scoping = scoping;
            this.special = special;
            this.fosterParenting = fosterParenting;
        }

        /**
         * @param name
         * @param node
         */
        StackNode(final String name, final S node) {
            this.name = name;
            this.node = node;
            this.scoping = ("table" == name || "caption" == name || "td" == name || "th" == name || "button" == name || "marquee" == name || "object" == name);
            this.special = ("address" == name || "area" == name || "base" == name || "basefont" == name || "bgsound" == name || "blockquote" == name || "body" == name || "br" == name || "center" == name || "col" == name || "colgroup" == name || "dd" == name || "dir" == name || "div" == name || "dl" == name || "dt" == name || "embed" == name || "fieldset" == name || "form" == name || "frame" == name || "frameset" == name || "h1" == name || "h2" == name || "h3" == name || "h4" == name || "h5" == name || "h6" == name || "head" == name || "hr" == name || "iframe" == name || "image" == name || "img" == name || "input" == name || "isindex" == name || "li" == name || "link" == name || "listing" == name || "menu" == name || "meta" == name || "noembed" == name || "noframes" == name || "noscript" == name || "ol" == name || "optgroup" == name || "option" == name || "p" == name || "param" == name || "plaintext" == name || "pre" == name || "script" == name || "select" == name || "spacer" == name || "style" == name || "tbody" == name || "textarea" == name || "tfoot" == name || "thead" == name || "title" == name || "tr" == name || "ul" == name ||  "wbr" == name);
            this.fosterParenting = ("table" == name || "tbody" == name || "tfoot" == name || "thead" == name || "tr" == name);
        }
    }
    
    private final static char[] ISINDEX_PROMPT = "This is a searchable index. Insert your search keywords here: ".toCharArray();

    private final static String[] QUIRKY_PUBLIC_IDS = {
            "+//silmaril//dtd html pro v0r11 19970101//en",
            "-//advasoft ltd//dtd html 3.0 aswedit + extensions//en",
            "-//as//dtd html 3.0 aswedit + extensions//en",
            "-//ietf//dtd html 2.0 level 1//en",
            "-//ietf//dtd html 2.0 level 2//en",
            "-//ietf//dtd html 2.0 strict level 1//en",
            "-//ietf//dtd html 2.0 strict level 2//en",
            "-//ietf//dtd html 2.0 strict//en", "-//ietf//dtd html 2.0//en",
            "-//ietf//dtd html 2.1e//en", "-//ietf//dtd html 3.0//en",
            "-//ietf//dtd html 3.0//en//", "-//ietf//dtd html 3.2 final//en",
            "-//ietf//dtd html 3.2//en", "-//ietf//dtd html 3//en",
            "-//ietf//dtd html level 0//en",
            "-//ietf//dtd html level 0//en//2.0",
            "-//ietf//dtd html level 1//en",
            "-//ietf//dtd html level 1//en//2.0",
            "-//ietf//dtd html level 2//en",
            "-//ietf//dtd html level 2//en//2.0",
            "-//ietf//dtd html level 3//en",
            "-//ietf//dtd html level 3//en//3.0",
            "-//ietf//dtd html strict level 0//en",
            "-//ietf//dtd html strict level 0//en//2.0",
            "-//ietf//dtd html strict level 1//en",
            "-//ietf//dtd html strict level 1//en//2.0",
            "-//ietf//dtd html strict level 2//en",
            "-//ietf//dtd html strict level 2//en//2.0",
            "-//ietf//dtd html strict level 3//en",
            "-//ietf//dtd html strict level 3//en//3.0",
            "-//ietf//dtd html strict//en",
            "-//ietf//dtd html strict//en//2.0",
            "-//ietf//dtd html strict//en//3.0", "-//ietf//dtd html//en",
            "-//ietf//dtd html//en//2.0", "-//ietf//dtd html//en//3.0",
            "-//metrius//dtd metrius presentational//en",
            "-//microsoft//dtd internet explorer 2.0 html strict//en",
            "-//microsoft//dtd internet explorer 2.0 html//en",
            "-//microsoft//dtd internet explorer 2.0 tables//en",
            "-//microsoft//dtd internet explorer 3.0 html strict//en",
            "-//microsoft//dtd internet explorer 3.0 html//en",
            "-//microsoft//dtd internet explorer 3.0 tables//en",
            "-//netscape comm. corp.//dtd html//en",
            "-//netscape comm. corp.//dtd strict html//en",
            "-//o'reilly and associates//dtd html 2.0//en",
            "-//o'reilly and associates//dtd html extended 1.0//en",
            "-//spyglass//dtd html 2.0 extended//en",
            "-//sq//dtd html 2.0 hotmetal + extensions//en",
            "-//sun microsystems corp.//dtd hotjava html//en",
            "-//sun microsystems corp.//dtd hotjava strict html//en",
            "-//w3c//dtd html 3 1995-03-24//en",
            "-//w3c//dtd html 3.2 draft//en", "-//w3c//dtd html 3.2 final//en",
            "-//w3c//dtd html 3.2//en", "-//w3c//dtd html 3.2s draft//en",
            "-//w3c//dtd html 4.0 frameset//en",
            "-//w3c//dtd html 4.0 transitional//en",
            "-//w3c//dtd html experimental 19960712//en",
            "-//w3c//dtd html experimental 970421//en",
            "-//w3c//dtd w3 html//en", "-//w3o//dtd w3 html 3.0//en",
            "-//w3o//dtd w3 html 3.0//en//",
            "-//w3o//dtd w3 html strict 3.0//en//",
            "-//webtechs//dtd mozilla html 2.0//en",
            "-//webtechs//dtd mozilla html//en",
            "-/w3c/dtd html 4.0 transitional/en", "html" };

    private static final int NOT_FOUND_ON_STACK = Integer.MAX_VALUE;
    
    private final StackNode<T> MARKER = new StackNode<T>(null, null);

    private final boolean nonConformingAndStreaming;

    private final boolean conformingAndStreaming;
    
    private final boolean coalescingText;   
    
    private Phase phase = Phase.INITIAL;

    private Phase phaseBeforeSwitchingToTrailingEnd;

    protected Tokenizer tokenizer;

    private ErrorHandler errorHandler;

    private DocumentModeHandler documentModeHandler;

    private DoctypeExpectation doctypeExpectation;

    private int cdataOrRcdataTimesToPop;

    private boolean scriptingEnabled;
    
    private boolean needToDropLF;

    private boolean wantingComments;

    private boolean fragment;

    private StackNode<T> context;
    
    private Phase previousPhaseBeforeTrailingEnd;
    
    private StackNode<T>[] stack;
    
    private int currentPtr = -1;

    private StackNode<T>[] listOfActiveFormattingElements;

    private int listPtr = -1;
    
    private T formPointer;

    private T headPointer;
    
    protected TreeBuilder(XmlViolationPolicy streamabilityViolationPolicy, boolean coalescingText) {
        this.conformingAndStreaming = streamabilityViolationPolicy == XmlViolationPolicy.FATAL;
        this.nonConformingAndStreaming = streamabilityViolationPolicy == XmlViolationPolicy.ALTER_INFOSET;
        this.coalescingText = coalescingText;
    }
    
    /**
     * Reports an condition that would make the infoset incompatible with XML
     * 1.0 as fatal.
     * 
     * @throws SAXException
     * @throws SAXParseException
     */
    protected final void fatal() throws SAXException {
        if (errorHandler == null) {
            return;
        }
        SAXParseException spe = new SAXParseException("Last error required non-streamable recovery.", tokenizer);
        errorHandler.fatalError(spe);
        throw spe;
    }

    /**
     * Reports a Parse Error.
     * 
     * @param message
     *            the message
     * @throws SAXException
     */
    protected final void err(String message) throws SAXException {
        if (errorHandler == null) {
            return;
        }
        SAXParseException spe = new SAXParseException(message, tokenizer);
        errorHandler.error(spe);
    }

    /**
     * Reports a warning
     * 
     * @param message
     *            the message
     * @throws SAXException
     */
    protected final void warn(String message) throws SAXException {
        if (errorHandler == null) {
            return;
        }
        SAXParseException spe = new SAXParseException(message, tokenizer);
        errorHandler.warning(spe);
    }

    public final void start(Tokenizer self) throws SAXException {
        phase = Phase.INITIAL;
        tokenizer = self;
        stack  = new StackNode[64];
        needToDropLF = false;
        cdataOrRcdataTimesToPop = 0;
        currentPtr = -1;
        formPointer = null;
        start();
    }

    public final void doctype(String name, String publicIdentifier,
            String systemIdentifier, boolean correct) throws SAXException {
        needToDropLF = false;
        switch (phase) {
            case INITIAL:
                /*
                 * A DOCTYPE token If the DOCTYPE token's name does not
                 * case-insensitively match the string "HTML", or if the token's
                 * public identifier is not missing, or if the token's system
                 * identifier is not missing, then there is a parse error.
                 * Conformance checkers may, instead of reporting this error,
                 * switch to a conformance checking mode for another language
                 * (e.g. based on the DOCTYPE token a conformance checker could
                 * recognise that the document is an HTML4-era document, and
                 * defer to an HTML4 conformance checker.)
                 * 
                 * Append a DocumentType node to the Document node, with the
                 * name attribute set to the name given in the DOCTYPE token;
                 * the publicId attribute set to the public identifier given in
                 * the DOCTYPE token, or the empty string if the public
                 * identifier was not set; the systemId attribute set to the
                 * system identifier given in the DOCTYPE token, or the empty
                 * string if the system identifier was not set; and the other
                 * attributes specific to DocumentType objects set to null and
                 * empty lists as appropriate. Associate the DocumentType node
                 * with the Document object so that it is returned as the value
                 * of the doctype attribute of the Document object.
                 */
                appendDoctypeToDocument(name, publicIdentifier == null ? ""
                        : publicIdentifier, systemIdentifier == null ? ""
                        : systemIdentifier);
                /*
                 * Then, if the DOCTYPE token matches one of the conditions in
                 * the following list, then set the document to quirks mode:
                 * 
                 * Otherwise, if the DOCTYPE token matches one of the conditions
                 * in the following list, then set the document to limited
                 * quirks mode: + The public identifier is set to: "-//W3C//DTD
                 * XHTML 1.0 Frameset//EN" + The public identifier is set to:
                 * "-//W3C//DTD XHTML 1.0 Transitional//EN" + The system
                 * identifier is not missing and the public identifier is set
                 * to: "-//W3C//DTD HTML 4.01 Frameset//EN" + The system
                 * identifier is not missing and the public identifier is set
                 * to: "-//W3C//DTD HTML 4.01 Transitional//EN"
                 * 
                 * The name, system identifier, and public identifier strings
                 * must be compared to the values given in the lists above in a
                 * case-insensitive manner.
                 */
                String publicIdentifierLC = toAsciiLowerCase(publicIdentifier);
                String systemIdentifierLC = toAsciiLowerCase(systemIdentifier);
                switch (doctypeExpectation) {
                    case HTML:
                        if (isQuirky(name, publicIdentifierLC,
                                systemIdentifierLC, correct)) {
                            err("Quirky doctype.");
                            documentMode(DocumentMode.QUIRKS_MODE,
                                    publicIdentifier, systemIdentifier, false);
                        } else if (isAlmostStandards(publicIdentifierLC,
                                systemIdentifierLC)) {
                            err("Almost standards mode doctype.");
                            documentMode(DocumentMode.ALMOST_STANDARDS_MODE,
                                    publicIdentifier, systemIdentifier, false);
                        } else {
                            if (!(publicIdentifier == null && systemIdentifier == null)) {
                                err("Legacy doctype.");
                            }
                            documentMode(DocumentMode.STANDARDS_MODE,
                                    publicIdentifier, systemIdentifier, false);
                        }
                        break;
                    case HTML401_STRICT:
                        tokenizer.turnOnAdditionalHtml4Errors();
                        if (isQuirky(name, publicIdentifierLC,
                                systemIdentifierLC, correct)) {
                            err("Quirky doctype.");
                            documentMode(DocumentMode.QUIRKS_MODE,
                                    publicIdentifier, systemIdentifier, true);
                        } else if (isAlmostStandards(publicIdentifierLC,
                                systemIdentifierLC)) {
                            err("Almost standards mode doctype.");
                            documentMode(DocumentMode.ALMOST_STANDARDS_MODE,
                                    publicIdentifier, systemIdentifier, true);
                        } else {
                            if ("-//W3C//DTD HTML 4.01//EN".equals(publicIdentifier)) {
                                if (!"http://www.w3.org/TR/html4/strict.dtd".equals(systemIdentifier)) {
                                    warn("The doctype did not contain the system identifier prescribed by the HTML 4.01 specification.");
                                }
                            } else {
                                err("The doctype was not the HTML 4.01 Strict doctype.");
                            }
                            documentMode(DocumentMode.STANDARDS_MODE,
                                    publicIdentifier, systemIdentifier, true);
                        }
                        break;
                    case HTML401_TRANSITIONAL:
                        tokenizer.turnOnAdditionalHtml4Errors();
                        if (isQuirky(name, publicIdentifierLC,
                                systemIdentifierLC, correct)) {
                            err("Quirky doctype.");
                            documentMode(DocumentMode.QUIRKS_MODE,
                                    publicIdentifier, systemIdentifier, true);
                        } else if (isAlmostStandards(publicIdentifierLC,
                                systemIdentifierLC)) {
                            if ("-//W3C//DTD HTML 4.01 Transitional//EN".equals(publicIdentifier)
                                    && systemIdentifier != null) {
                                if (!"http://www.w3.org/TR/html4/loose.dtd".equals(systemIdentifier)) {
                                    warn("The doctype did not contain the system identifier prescribed by the HTML 4.01 specification.");
                                }
                            } else {
                                err("The doctype was not a non-quirky HTML 4.01 Transitional doctype.");
                            }
                            documentMode(DocumentMode.ALMOST_STANDARDS_MODE,
                                    publicIdentifier, systemIdentifier, true);
                        } else {
                            err("The doctype was not the HTML 4.01 Transitional doctype.");
                            documentMode(DocumentMode.STANDARDS_MODE,
                                    publicIdentifier, systemIdentifier, true);
                        }
                        break;
                    case AUTO:
                        if (isQuirky(name, publicIdentifierLC,
                                systemIdentifierLC, correct)) {
                            err("Quirky doctype.");
                            documentMode(DocumentMode.QUIRKS_MODE,
                                    publicIdentifier, systemIdentifier, false);
                        } else if (isAlmostStandards(publicIdentifierLC,
                                systemIdentifierLC)) {
                            boolean html4 = "-//W3C//DTD HTML 4.01 Transitional//EN".equals(publicIdentifier);
                            if (html4) {
                                tokenizer.turnOnAdditionalHtml4Errors();
                                if (!"http://www.w3.org/TR/html4/loose.dtd".equals(systemIdentifier)) {
                                    warn("The doctype did not contain the system identifier prescribed by the HTML 4.01 specification.");
                                }
                            } else {
                                err("Almost standards mode doctype.");
                            }
                            documentMode(DocumentMode.ALMOST_STANDARDS_MODE,
                                    publicIdentifier, systemIdentifier, html4);
                        } else {
                            boolean html4 = "-//W3C//DTD HTML 4.01//EN".equals(publicIdentifier);
                            if (html4) {
                                tokenizer.turnOnAdditionalHtml4Errors();
                                if (!"http://www.w3.org/TR/html4/strict.dtd".equals(systemIdentifier)) {
                                    warn("The doctype did not contain the system identifier prescribed by the HTML 4.01 specification.");
                                }
                            } else {
                                if (!(publicIdentifier == null && systemIdentifier == null)) {
                                    err("Legacy doctype.");
                                }
                            }
                            documentMode(DocumentMode.STANDARDS_MODE,
                                    publicIdentifier, systemIdentifier, html4);
                        }
                        break;
                    case NO_DOCTYPE_ERRORS:
                        if (isQuirky(name, publicIdentifierLC,
                                systemIdentifierLC, correct)) {
                            documentMode(DocumentMode.QUIRKS_MODE,
                                    publicIdentifier, systemIdentifier, false);
                        } else if (isAlmostStandards(publicIdentifierLC,
                                systemIdentifierLC)) {
                            documentMode(DocumentMode.ALMOST_STANDARDS_MODE,
                                    publicIdentifier, systemIdentifier, false);
                        } else {
                            documentMode(DocumentMode.STANDARDS_MODE,
                                    publicIdentifier, systemIdentifier, false);
                        }
                        break;
                }

                /*
                 * 
                 * Then, switch to the root element phase of the tree
                 * construction stage.
                 * 
                 * 
                 */
                phase = Phase.ROOT_ELEMENT;
                return;
            default:
                /*
                 * A DOCTYPE token Parse error.
                 */
                err("Stray doctype.");
                /*
                 * Ignore the token.
                 */
                return;
        }
    }

    public final void comment(char[] buf, int length) throws SAXException {
        needToDropLF = false;
        if (wantingComments) {
            switch (phase) {
                case INITIAL:
                case ROOT_ELEMENT:
                case TRAILING_END:
                    /*
                     * A comment token Append a Comment node to the Document
                     * object with the data attribute set to the data given in
                     * the comment token.
                     */
                    appendCommentToDocument(buf, length);
                    return;
                case AFTER_BODY:
                    /*
                     * * A comment token Append a Comment node to the first
                     * element in the stack of open elements (the html element),
                     * with the data attribute set to the data given in the
                     * comment token.
                     * 
                     */
                    appendCommentToRootElement(buf, length);
                    return;
                default:
                    /*
                     * * A comment token Append a Comment node to the current
                     * node with the data attribute set to the data given in the
                     * comment token.
                     * 
                     */
                    appendCommentToCurrentNode(buf, length);
                    return;
            }
        }
    }

    /**
     * @see nu.validator.htmlparser.TokenHandler#characters(char[], int, int)
     */
    public final void characters(char[] buf, int start, int length)
            throws SAXException {
        if (needToDropLF) {
            if (buf[start] == '\n') {
                start++;
                length--;
                if (length == 0) {
                    return;
                }
            }
            needToDropLF = false;
        } else if (cdataOrRcdataTimesToPop > 0) {
            appendCharactersToCurrentNode(buf, start, length);
            return;
        }

        // optimize the most common case
        if (phase == Phase.IN_BODY || phase == Phase.IN_CELL
                || phase == Phase.IN_CAPTION) {
            reconstructTheActiveFormattingElements();
            appendCharactersToCurrentNode(buf, start, length);
            return;
        }

        int end = start + length;
        loop: for (int i = start; i < end; i++) {
            switch (buf[i]) {
                case ' ':
                case '\t':
                case '\n':
                case '\u000B':
                case '\u000C':
                    /*
                     * A character token that is one of one of U+0009 CHARACTER
                     * TABULATION, U+000A LINE FEED (LF), U+000B LINE
                     * TABULATION, U+000C FORM FEED (FF), or U+0020 SPACE
                     */
                    switch (phase) {
                        case INITIAL:
                        case ROOT_ELEMENT:
                            /*
                             * Ignore the token.
                             */
                            start = i + 1;
                            continue;
                        case BEFORE_HEAD:
                        case IN_HEAD:
                        case IN_HEAD_NOSCRIPT:
                        case AFTER_HEAD:
                        case IN_TABLE:
                        case IN_COLUMN_GROUP:
                        case IN_TABLE_BODY:
                        case IN_ROW:
                        case IN_FRAMESET:
                        case AFTER_FRAMESET:
                            /*
                             * Append the character to the current node.
                             */
                            continue;
                        case IN_BODY:
                        case IN_CELL:
                        case IN_CAPTION:
                            // XXX is this dead code?
                            if (start < i) {
                                appendCharactersToCurrentNode(buf, start, i
                                        - start);
                                start = i;
                            }

                            /*
                             * Reconstruct the active formatting elements, if
                             * any.
                             */
                            reconstructTheActiveFormattingElements();
                            /* Append the token's character to the current node. */
                            break loop;
                        case IN_SELECT:
                            break loop;
                        case AFTER_BODY:
                            if (start < i) {
                                appendCharactersToCurrentNode(buf, start, i
                                        - start);
                                start = i;
                            }
                            /*
                             * Reconstruct the active formatting elements, if
                             * any.
                             */
                            reconstructTheActiveFormattingElements();
                            /* Append the token's character to the current node. */
                            continue;
                        case TRAILING_END:
                            if (phaseBeforeSwitchingToTrailingEnd == Phase.AFTER_FRAMESET) {
                                continue;
                            } else {
                                if (start < i) {
                                    appendCharactersToCurrentNode(buf, start, i
                                            - start);
                                    start = i;
                                }
                                /*
                                 * Reconstruct the active formatting elements,
                                 * if any.
                                 */
                                reconstructTheActiveFormattingElements();
                                /*
                                 * Append the token's character to the current
                                 * node.
                                 */
                                continue;
                            }
                    }
                default:
                    /*
                     * A character token that is not one of one of U+0009
                     * CHARACTER TABULATION, U+000A LINE FEED (LF), U+000B LINE
                     * TABULATION, U+000C FORM FEED (FF), or U+0020 SPACE
                     */
                    switch (phase) {
                        case INITIAL:
                            /*
                             * Parse error.
                             */
                            if (doctypeExpectation != DoctypeExpectation.NO_DOCTYPE_ERRORS) {
                                err("Non-space characters found without seeing a doctype first.");
                            }
                            /*
                             * 
                             * Set the document to quirks mode.
                             */
                            documentMode(DocumentMode.QUIRKS_MODE, null, null,
                                    false);
                            /*
                             * Then, switch to the root element phase of the
                             * tree construction stage
                             */
                            phase = Phase.ROOT_ELEMENT;
                            /*
                             * and reprocess the current token.
                             * 
                             * 
                             */
                            i--;
                            continue;
                        case ROOT_ELEMENT:
                            /*
                             * Create an HTMLElement node with the tag name
                             * html, in the HTML namespace. Append it to the
                             * Document object.
                             */
                            appendHtmlElementToDocument();
                            /* Switch to the main phase */
                            phase = Phase.BEFORE_HEAD;
                            /*
                             * reprocess the current token.
                             * 
                             */
                            i--;
                            continue;
                        case BEFORE_HEAD:
                            if (start < i) {
                                appendCharactersToCurrentNode(buf, start, i
                                        - start);
                                start = i;
                            }
                            /*
                             * /*Act as if a start tag token with the tag name
                             * "head" and no attributes had been seen,
                             */
                            appendToCurrentNodeAndPushHeadElement(EmptyAttributes.EMPTY_ATTRIBUTES);
                            phase = Phase.IN_HEAD;
                            /*
                             * then reprocess the current token.
                             * 
                             * This will result in an empty head element being
                             * generated, with the current token being
                             * reprocessed in the "after head" insertion mode.
                             */
                            i--;
                            continue;
                        case IN_HEAD:
                            if (start < i) {
                                appendCharactersToCurrentNode(buf, start, i
                                        - start);
                                start = i;
                            }
                            /*
                             * Act as if an end tag token with the tag name
                             * "head" had been seen,
                             */
                            popCurrentNode();
                            phase = Phase.AFTER_HEAD;
                            /*
                             * and reprocess the current token.
                             */
                            i--;
                            continue;
                        case IN_HEAD_NOSCRIPT:
                            if (start < i) {
                                appendCharactersToCurrentNode(buf, start, i
                                        - start);
                                start = i;
                            }
                            /*
                             * Parse error. Act as if an end tag with the tag
                             * name "noscript" had been seen
                             */
                            err("Non-space character inside \u201Cnoscript\u201D inside \u201Chead\u201D.");
                            popCurrentNode();
                            phase = Phase.IN_HEAD;
                            /*
                             * and reprocess the current token.
                             */
                            i--;
                            continue;
                        case AFTER_HEAD:
                            if (start < i) {
                                appendCharactersToCurrentNode(buf, start, i
                                        - start);
                                start = i;
                            }
                            /*
                             * Act as if a start tag token with the tag name
                             * "body" and no attributes had been seen,
                             */
                            appendToCurrentNodeAndPushBodyElement();
                            phase = Phase.IN_BODY;
                            /*
                             * and then reprocess the current token.
                             */
                            i--;
                            continue;
                        case IN_BODY:
                        case IN_CELL:
                        case IN_CAPTION:
                            if (start < i) {
                                appendCharactersToCurrentNode(buf, start, i
                                        - start);
                                start = i;
                            }
                            /*
                             * Reconstruct the active formatting elements, if
                             * any.
                             */
                            reconstructTheActiveFormattingElements();
                            /* Append the token's character to the current node. */
                            break loop;
                        case IN_TABLE:
                        case IN_TABLE_BODY:
                        case IN_ROW:
                            if (start < i) {
                                appendCharactersToCurrentNode(buf, start, i
                                        - start);
                            }
                            reconstructTheActiveFormattingElements();
                            appendCharToFosterParent(buf[i]);
                            start = i + 1;
                            continue;
                        case IN_COLUMN_GROUP:
                            /*
                             * Act as if an end tag with the tag name "colgroup"
                             * had been seen, and then, if that token wasn't
                             * ignored, reprocess the current token.
                             */
                            if (currentPtr == 0) {
                                err("Non-space in \u201Ccolgroup\u201D when parsing fragment.");
                                continue;
                            }
                            popCurrentNode();
                            phase = Phase.IN_TABLE;
                            i--;
                            continue;
                        case IN_SELECT:
                            break loop;
                        case AFTER_BODY:
                            err("Non-space character after body.");
                            phase = Phase.IN_BODY;
                            i--;
                            continue;
                        case IN_FRAMESET:
                            if (start < i) {
                                appendCharactersToCurrentNode(buf, start, i
                                        - start);
                                start = i;
                            }
                            /*
                             * Parse error.
                             */
                            err("Non-space in \u201Cframeset\u201D.");
                            /*
                             * Ignore the token.
                             */
                            start = i + 1;
                            continue;
                        case AFTER_FRAMESET:
                            if (start < i) {
                                appendCharactersToCurrentNode(buf, start, i
                                        - start);
                                start = i;
                            }
                            /*
                             * Parse error.
                             */
                            err("Non-space after \u201Cframeset\u201D.");
                            /*
                             * Ignore the token.
                             */
                            start = i + 1;
                            continue;
                        case TRAILING_END:
                            /*
                             * Parse error.
                             */
                            err("Non-space character in page trailer.");
                            /*
                             * Switch back to the main phase and reprocess the
                             * token.
                             */
                            phase = phaseBeforeSwitchingToTrailingEnd;
                            i--;
                            continue;
                    }
            }
        }
        if (start < end) {
            appendCharactersToCurrentNode(buf, start, end - start);
        }
    }

    public final void eof() throws SAXException {
        for (;;) {
            switch (phase) {
                case INITIAL:
                    /*
                     * Parse error.
                     */
                    if (doctypeExpectation != DoctypeExpectation.NO_DOCTYPE_ERRORS) {
                        err("End of file seen without seeing a doctype first.");
                    }
                    /*
                     * 
                     * Set the document to quirks mode.
                     */
                    documentMode(DocumentMode.QUIRKS_MODE, null, null, false);
                    /*
                     * Then, switch to the root element phase of the tree
                     * construction stage
                     */
                    phase = Phase.ROOT_ELEMENT;
                    /*
                     * and reprocess the current token.
                     */
                    continue;
                case ROOT_ELEMENT:
                    /*
                     * Create an HTMLElement node with the tag name html, in the
                     * HTML namespace. Append it to the Document object.
                     */
                    appendHtmlElementToDocument();
                    /* Switch to the main phase */
                    phase = Phase.BEFORE_HEAD;
                    /*
                     * reprocess the current token.
                     */
                    continue;
                case BEFORE_HEAD:
                case IN_HEAD:
                case IN_HEAD_NOSCRIPT:
                case AFTER_HEAD:
                case IN_BODY:
                case IN_TABLE:
                case IN_CAPTION:
                case IN_COLUMN_GROUP:
                case IN_TABLE_BODY:
                case IN_ROW:
                case IN_CELL:
                case IN_SELECT:
                case AFTER_BODY:
                case IN_FRAMESET:
                case AFTER_FRAMESET:
                    /*
                     * Generate implied end tags.
                     */
                    generateImpliedEndTags();
                    /*
                     * If there are more than two nodes on the stack of open
                     * elements,
                     */
                    if (currentPtr > 1) {
                        err("End of file seen and there were open elements.");
                    } else if (currentPtr == 1
                            && stack[1].name != "body") {
                        /*
                         * or if there are two nodes but the second node is not
                         * a body node, this is a parse error.
                         */
                        err("End of file seen and there were open elements.");
                    }
                    if (fragment) {
                        if (currentPtr > 0 && stack[1].name != "body") {
                            /*
                             * Otherwise, if the parser was originally created as part
                             * of the HTML fragment parsing algorithm, and there's more
                             * than one element in the stack of open elements, and the
                             * second node on the stack of open elements is not a body
                             * node, then this is a parse error. (fragment case)
                             */
                            err("End of file seen and there were open elements.");
                        }                        
                    }

                    /* Stop parsing. */
                    end();
                    return;
                    /*
                     * This fails because it doesn't imply HEAD and BODY tags.
                     * We should probably expand out the insertion modes and
                     * merge them with phases and then put the three things here
                     * into each insertion mode instead of trying to factor them
                     * out so carefully.
                     * 
                     */
                case TRAILING_END:
                    /* Stop parsing. */
                    end();
                    return;
            }
        }
    }

    public final void startTag(String name, Attributes attributes)
            throws SAXException {
        needToDropLF = false;
        for (;;) {
            switch (phase) {
                case IN_TABLE_BODY:
                    if ("tr" == name) {
                        clearStackBackTo(findLastInTableScopeOrRootTbodyTheadTfoot());
                        appendToCurrentNodeAndPushElement(name, attributes);
                        phase = Phase.IN_ROW;
                        return;
                    } else if ("td" == name || "th" == name) {
                        err("\u201C" + name + "\u201D start tag in table body.");
                        clearStackBackTo(findLastInTableScopeOrRootTbodyTheadTfoot());
                        appendToCurrentNodeAndPushElement("tr",
                                EmptyAttributes.EMPTY_ATTRIBUTES);
                        phase = Phase.IN_ROW;
                        continue;
                    } else if ("caption" == name || "col" == name
                            || "colgroup" == name || "tbody" == name
                            || "tfoot" == name || "thead" == name) {
                        int eltPos = findLastInTableScopeOrRootTbodyTheadTfoot();
                        if (eltPos == 0) {
                            err("Stray \u201C" + name + "\u201D start tag.");
                            return;
                        } else {
                            clearStackBackTo(eltPos);
                            popCurrentNode();
                            phase = Phase.IN_TABLE;
                            continue;
                        }
                    } else {
                        // fall through to IN_TABLE
                    }
                case IN_ROW:
                    if ("td" == name || "th" == name) {
                        clearStackBackTo(findLastOrRoot("tr"));
                        appendToCurrentNodeAndPushElement(name, attributes);
                        phase = Phase.IN_CELL;
                        insertMarker();
                        return;
                    } else if ("caption" == name || "col" == name
                            || "colgroup" == name || "tbody" == name
                            || "tfoot" == name || "thead" == name
                            || "tr" == name) {
                        int eltPos = findLastOrRoot("tr");
                        if (eltPos == 0) {
                            assert fragment;
                            err("No table row to close.");
                            return;
                        }
                        clearStackBackTo(eltPos);
                        popCurrentNode();
                        phase = Phase.IN_TABLE_BODY;
                        continue;
                    } else {
                        // fall through to IN_TABLE
                    }
                case IN_TABLE:
                    if ("caption" == name) {
                        clearStackBackTo(findLastOrRoot("table"));
                        insertMarker();
                        appendToCurrentNodeAndPushElement(name, attributes);
                        phase = Phase.IN_CAPTION;
                        return;
                    } else if ("colgroup" == name) {
                        clearStackBackTo(findLastOrRoot("table"));
                        appendToCurrentNodeAndPushElement(name, attributes);
                        phase = Phase.IN_COLUMN_GROUP;
                        return;
                    } else if ("col" == name) {
                        clearStackBackTo(findLastOrRoot("table"));
                        appendToCurrentNodeAndPushElement("colgroup",
                                EmptyAttributes.EMPTY_ATTRIBUTES);
                        phase = Phase.IN_COLUMN_GROUP;
                        continue;
                    } else if ("tbody" == name || "tfoot" == name
                            || "thead" == name) {
                        clearStackBackTo(findLastOrRoot("table"));
                        appendToCurrentNodeAndPushElement(name, attributes);
                        phase = Phase.IN_TABLE_BODY;
                        return;
                    } else if ("td" == name || "tr" == name || "th" == name) {
                        clearStackBackTo(findLastOrRoot("table"));
                        appendToCurrentNodeAndPushElement("tbody",
                                EmptyAttributes.EMPTY_ATTRIBUTES);
                        phase = Phase.IN_TABLE_BODY;
                        continue;
                    } else if ("table" == name) {
                        err("Start tag for \u201Ctable\u201D seen but the previous \u201Ctable\u201D is still open.");
                        int eltPos = findLastInTableScope(name);
                        if (eltPos == NOT_FOUND_ON_STACK) {
                            assert fragment;
                            return;
                        }
                        generateImpliedEndTags();
                        // XXX is the next if dead code?
                        if (!isCurrent("table")) {
                            err("Unclosed elements on stack.");
                        }
                        while (currentPtr >= eltPos) {
                            popCurrentNode();
                        }
                        resetTheInsertionMode();
                        continue;
                    } else {
                        err("Start tag \u201C" + name
                                + "\u201D seen in \u201Ctable\u201D.");
                        // fall through to IN_BODY
                    }
                case IN_CAPTION:
                    if ("caption" == name || "col" == name
                            || "colgroup" == name || "tbody" == name
                            || "td" == name || "tfoot" == name || "th" == name
                            || "thead" == name || "tr" == name) {
                        err("Stray \u201C" + name
                                + "\u201D start tag in \u201Ccaption\u201D.");
                        int eltPos = findLastInTableScope("caption");
                        if (eltPos == NOT_FOUND_ON_STACK) {
                            return;
                        }
                        generateImpliedEndTags();
                        if (currentPtr != eltPos) {
                            err("Unclosed elements on stack.");
                        }
                        while (currentPtr >= eltPos) {
                            popCurrentNode();
                        }
                        clearTheListOfActiveFormattingElementsUpToTheLastMarker();
                        phase = Phase.IN_TABLE;
                        continue;
                    } else {
                        // fall through to IN_BODY
                    }
                case IN_CELL:
                    if ("caption" == name || "col" == name
                            || "colgroup" == name || "tbody" == name
                            || "td" == name || "tfoot" == name || "th" == name
                            || "thead" == name || "tr" == name) {
                        int eltPos = findLastInTableScopeTdTh();
                        if (eltPos == NOT_FOUND_ON_STACK) {
                            err("No cell to close.");
                            return;
                        } else {
                            closeTheCell(eltPos);
                            continue;
                        }
                    } else {
                        // fall through to IN_BODY
                    }
                case IN_BODY:
                    if ("base" == name || "link" == name || "meta" == name
                            || "style" == name || "script" == name) {
                        // Fall through to IN_HEAD
                    } else if ("title" == name) {
                        err("\u201Ctitle\u201D element found inside \u201Cbody\201D.");
                        if (nonConformingAndStreaming) {
                            pushHeadPointerOntoStack();
                        }
                        appendToCurrentNodeAndPushElement(name, attributes);
                        cdataOrRcdataTimesToPop = nonConformingAndStreaming ? 1
                                : 2; // pops head
                        tokenizer.setContentModelFlag(ContentModelFlag.RCDATA,
                                name);
                        return;
                    } else if ("body" == name) {
                        err("\u201Cbody\u201D start tag found but the \u201Cbody\201D element is already open.");
                        addAttributesToBody(attributes);
                        return;
                    } else if ("p" == name || "div" == name || "h1" == name
                            || "h2" == name || "h3" == name || "h4" == name
                            || "h5" == name || "h6" == name
                            || "blockquote" == name || "ol" == name
                            || "ul" == name || "dl" == name
                            || "fieldset" == name || "address" == name
                            || "menu" == name || "center" == name
                            || "dir" == name || "listing" == name) {
                        implicitlyCloseP();
                        appendToCurrentNodeAndPushElement(name, attributes);
                        return;
                    } else if ("pre" == name) {
                        implicitlyCloseP();
                        appendToCurrentNodeAndPushElement(name, attributes);
                        needToDropLF = true;
                        return;
                    } else if ("form" == name) {
                        if (formPointer == null) {
                            err("Saw a \u201Cform\u201D start tag, but there was already an active \u201Cform\u201D element.");
                            return;
                        } else {
                            implicitlyCloseP();
                            appendToCurrentNodeAndPushFormElement(attributes);
                            return;
                        }
                    } else if ("li" == name) {
                        implicitlyCloseP();
                        int eltPos = findLiToPop();
                        if (eltPos < currentPtr) {
                            err("A \u201Cli\u201D start tag was seen but the previous \u201Cli\u201D element had open children.");
                        }
                        while (currentPtr >= eltPos) {
                            popCurrentNode();
                        }
                        appendToCurrentNodeAndPushElement(name, attributes);
                        return;
                    } else if ("dd" == name || "dt" == name) {
                        implicitlyCloseP();
                        int eltPos = findDdOrDtToPop();
                        if (eltPos < currentPtr) {
                            err("A definition list item start tag was seen but the previous definition list item element had open children.");
                        }
                        while (currentPtr >= eltPos) {
                            popCurrentNode();
                        }
                        appendToCurrentNodeAndPushElement(name, attributes);
                        return;
                    } else if ("plaintext" == name) {
                        implicitlyCloseP();
                        appendToCurrentNodeAndPushElement(name, attributes);
                        tokenizer.setContentModelFlag(
                                ContentModelFlag.PLAINTEXT, name);
                        return;
                    } else if ("a" == name) {
                        int activeA = findInListOfActiveFormattingElementsContainsBetweenEndAndLastMarker("a");
                        if (activeA == -1) {
                            err("An \u201Ca\u201D start tag seen with already an active \u201Ca\u201D element.");
                            adoptionAgencyEndTag("a");
                            removeFromStack(listOfActiveFormattingElements[activeA]);
                            removeFromListOfActiveFormattingElements(activeA);
                        }
                        reconstructTheActiveFormattingElements();
                        appendToCurrentNodeAndPushFormattingElement(name,
                                attributes);
                        return;
                    } else if ("i" == name || "b" == name || "em" == name
                            || "strong" == name || "font" == name
                            || "big" == name || "s" == name || "small" == name
                            || "strike" == name || "tt" == name || "u" == name) {
                        reconstructTheActiveFormattingElements();
                        appendToCurrentNodeAndPushFormattingElement(name,
                                attributes);
                        return;
                    } else if ("nobr" == name) {
                        reconstructTheActiveFormattingElements();
                        if (NOT_FOUND_ON_STACK != findLastInScope("nobr")) {
                            err("\u201Cnobr\u201D start tag seen when there was an open \u201Cnobr\u201D element in scope.");
                            adoptionAgencyEndTag("nobr");
                        }
                        appendToCurrentNodeAndPushFormattingElement(name,
                                attributes);
                        return;
                    } else if ("button" == name) {
                        int eltPos = findLastInScope(name);
                        if (eltPos != NOT_FOUND_ON_STACK) {
                            err("\u201Cbutton\u201D start tag seen when there was an open \u201Cbutton\u201D element in scope.");
                            generateImpliedEndTags();
                            if (!isCurrent("button")) {
                                err("There was an open \u201Cbutton\u201D element in scope with unclosed children.");
                            }
                            while (currentPtr >= eltPos) {
                                popCurrentNode();
                            }
                            clearTheListOfActiveFormattingElementsUpToTheLastMarker();
                            continue;
                        } else {
                            reconstructTheActiveFormattingElements();
                            appendToCurrentNodeAndPushElement(name, attributes);
                            insertMarker();
                            return;
                        }
                    } else if ("object" == name || "marquee" == name) {
                        reconstructTheActiveFormattingElements();
                        appendToCurrentNodeAndPushElement(name, attributes);
                        insertMarker();
                        return;
                    } else if ("xmp" == name) {
                        reconstructTheActiveFormattingElements();
                        appendToCurrentNodeAndPushElement(name, attributes);
                        cdataOrRcdataTimesToPop = 1;
                        tokenizer.setContentModelFlag(ContentModelFlag.CDATA,
                                name);
                        return;
                    } else if ("table" == name) {
                        implicitlyCloseP();
                        appendToCurrentNodeAndPushElement(name, attributes);
                        phase = Phase.IN_TABLE;
                        return;
                    } else if ("br" == name || "img" == name || "embed" == name
                            || "param" == name || "area" == name
                            || "basefont" == name || "bgsound" == name
                            || "spacer" == name || "wbr" == name) {
                        reconstructTheActiveFormattingElements();
                        createElementAppendToCurrent(name, attributes);
                        return;
                    } else if ("hr" == name) {
                        implicitlyCloseP();
                        createElementAppendToCurrent(name, attributes);
                        return;
                    } else if ("image" == name) {
                        err("Saw a start tag \u201Cimage\201D.");
                        name = "img";
                        continue;
                    } else if ("input" == name) {
                        reconstructTheActiveFormattingElements();
                        createElementAppendToCurrent(name, attributes, formPointer);
                        return;
                    } else if ("isindex" == name) {
                        err("\u201Cisindex\201D seen.");
                        if (formPointer == null) {
                            return;
                        }
                        implicitlyCloseP();
                        AttributesImpl formAttrs = tokenizer.newAttributes();
                        int actionIndex = attributes.getIndex("action");
                        if (actionIndex > -1) {
                            formAttrs.addAttribute("action",
                                    attributes.getValue(actionIndex));
                        }
                        appendToCurrentNodeAndPushFormElement(formAttrs);
                        createElementAppendToCurrent("hr", EmptyAttributes.EMPTY_ATTRIBUTES);
                        appendToCurrentNodeAndPushElement("p",
                                EmptyAttributes.EMPTY_ATTRIBUTES);
                        appendToCurrentNodeAndPushElement("label",
                                EmptyAttributes.EMPTY_ATTRIBUTES);
                        int promptIndex = attributes.getIndex("prompt");
                        if (promptIndex > -1) {
                            char[] prompt = attributes.getValue(promptIndex).toCharArray();
                            appendCharactersToCurrentNode(prompt, 0,
                                    prompt.length);
                        } else {
                            // XXX localization
                            appendCharactersToCurrentNode(ISINDEX_PROMPT, 0,
                                    ISINDEX_PROMPT.length);
                        }
                        AttributesImpl inputAttributes = tokenizer.newAttributes();
                        for (int i = 0; i < attributes.getLength(); i++) {
                            String attributeQName = attributes.getQName(i);
                            if (!("name".equals(attributeQName)
                                    || "action".equals(attributeQName) || "prompt".equals(attributeQName))) {
                                inputAttributes.addAttribute(attributeQName,
                                        attributes.getValue(i));
                            }
                        }
                        createElementAppendToCurrent("input", inputAttributes, formPointer);
                        // XXX localization
                        popCurrentNode(); // label
                        popCurrentNode(); // p
                        createElementAppendToCurrent("hr", EmptyAttributes.EMPTY_ATTRIBUTES);
                        popCurrentNode(); // form
                        return;
                    } else if ("textarea" == name) {
                        appendToCurrentNodeAndPushElementWithFormPointer(name, attributes);
                        tokenizer.setContentModelFlag(ContentModelFlag.RCDATA,
                                name);
                        cdataOrRcdataTimesToPop = 1;
                        needToDropLF = true;
                        return;
                    } else if ("iframe" == name || "noembed" == name
                            || "noframes" == name
                            || ("noscript" == name && scriptingEnabled)) {
                        appendToCurrentNodeAndPushElement(name, attributes);
                        cdataOrRcdataTimesToPop = 1;
                        tokenizer.setContentModelFlag(ContentModelFlag.CDATA,
                                name);
                        return;
                    } else if ("select" == name) {
                        reconstructTheActiveFormattingElements();
                        // XXX form pointer
                        appendToCurrentNodeAndPushElement(name, attributes);
                        phase = Phase.IN_SELECT;
                        return;
                    } else if ("caption" == name || "col" == name
                            || "colgroup" == name || "frame" == name
                            || "frameset" == name || "head" == name
                            || "option" == name || "optgroup" == name
                            || "tbody" == name || "td" == name
                            || "tfoot" == name || "th" == name
                            || "thead" == name || "tr" == name) {
                        err("Stay start tag \u201C" + name + "\u201D.");
                        return;
                    } else {
                        reconstructTheActiveFormattingElements();
                        appendToCurrentNodeAndPushElement(name, attributes);
                        return;
                    }
                case IN_HEAD:
                    if ("base" == name) {
                        createElementAppendToCurrent(name, attributes);
                        return;
                    } else if ("meta" == name || "link" == name) {
                        // Fall through to IN_HEAD_NOSCRIPT
                    } else if ("title" == name) {
                        appendToCurrentNodeAndPushElement(name, attributes);
                        cdataOrRcdataTimesToPop = 1;
                        tokenizer.setContentModelFlag(ContentModelFlag.RCDATA,
                                name);
                        return;
                    } else if ("style" == name
                            || ("noscript" == name && scriptingEnabled)) {
                        appendToCurrentNodeAndPushElement(name, attributes);
                        cdataOrRcdataTimesToPop = 1;
                        tokenizer.setContentModelFlag(ContentModelFlag.CDATA,
                                name);
                        return;
                    } else if ("noscript" == name && !scriptingEnabled) {
                        appendToCurrentNodeAndPushElement(name, attributes);
                        phase = Phase.IN_HEAD_NOSCRIPT;
                        return;
                    } else if ("script" == name) {
                        // XXX need to manage much more stuff here if supporting
                        // document.write()
                        appendToCurrentNodeAndPushElement(name, attributes);
                        cdataOrRcdataTimesToPop = 1;
                        tokenizer.setContentModelFlag(ContentModelFlag.CDATA,
                                name);
                        return;
                    } else if ("head" == name) {
                        /* Parse error. */
                        err("Start tag for \u201Chead\u201D seen when \u201Chead\u201D was already open.");
                        /* Ignore the token. */
                        return;
                    } else {
                        popCurrentNode();
                        phase = Phase.AFTER_HEAD;
                        continue;
                    }
                case IN_HEAD_NOSCRIPT:
                    // XXX did Hixie really mean to omit "base" here?
                    if ("link" == name) {
                        createElementAppendToCurrent(name, attributes);
                        return;
                    } else if ("meta" == name) {
                        // XXX do charset stuff
                        createElementAppendToCurrent(name, attributes);
                        return;
                    } else if ("style" == name) {
                        appendToCurrentNodeAndPushElement(name, attributes);
                        cdataOrRcdataTimesToPop = 1;
                        tokenizer.setContentModelFlag(ContentModelFlag.CDATA,
                                name);
                        return;
                    } else if ("head" == name) {
                        err("Start tag for \u201Chead\u201D seen when \u201Chead\u201D was already open.");
                        return;
                    } else if ("noscript" == name) {
                        err("Start tag for \u201Cnoscript\u201D seen when \u201Cnoscript\u201D was already open.");
                        return;
                    } else {
                        err("Bad start tag in \u201Cnoscript\u201D in \u201Chead\u201D.");
                        popCurrentNode();
                        phase = Phase.IN_HEAD;
                        continue;
                    }
                case IN_COLUMN_GROUP:
                    if ("col" == name) {
                        createElementAppendToCurrent(name, attributes);
                        return;
                    } else {
                        if (currentPtr == 0) {
                            assert fragment;
                            err("Garbage in \u201Ccolgroup\u201D fragment.");
                            return;
                        }
                        popCurrentNode();
                        phase = Phase.IN_TABLE;
                        continue;
                    }
                case IN_SELECT:
                    if ("option" == name) {
                        if (isCurrent("option")) {
                            popCurrentNode();
                        }
                        appendToCurrentNodeAndPushElement(name, attributes);
                        return;
                    } else if ("optgroup" == name) {
                        if (isCurrent("option")) {
                            popCurrentNode();
                        }
                        if (isCurrent("optgroup")) {
                            popCurrentNode();
                        }
                        appendToCurrentNodeAndPushElement(name, attributes);
                        return;
                    } else if ("select" == name) {
                        err("\u201Cselect\u201D start tag where end tag expected.");
                        int eltPos = findLastInTableScope(name);
                        if (eltPos == NOT_FOUND_ON_STACK) {
                            assert fragment;
                            err("No \u201Cselect\u201D in table scope.");
                            return;
                        } else {
                            while (currentPtr >= eltPos) {
                                popCurrentNode();
                            }
                            resetTheInsertionMode();
                            return;
                        }
                    } else {
                        err("Stray \u201C" + name + "\u201D start tag.");
                        return;
                    }
                case AFTER_BODY:
                    err("Stray \u201C" + name + "\u201D start tag.");
                    phase = Phase.IN_BODY;
                    continue;
                case IN_FRAMESET:
                    if ("frameset" == name) {
                        appendToCurrentNodeAndPushElement(name, attributes);
                        return;
                    } else if ("frame" == name) {
                        createElementAppendToCurrent(name, attributes);
                        return;
                    } else {
                        // fall through to AFTER_FRAMESET
                    }
                case AFTER_FRAMESET:
                    if ("noframes" == name) {
                        appendToCurrentNodeAndPushElement(name, attributes);
                        cdataOrRcdataTimesToPop = 1;
                        tokenizer.setContentModelFlag(ContentModelFlag.CDATA,
                                name);
                        return;
                    } else {
                        err("Stray \u201C" + name + "\u201D start tag.");
                        return;
                    }
                case INITIAL:
                    /*
                     * Parse error.
                     */
                    if (doctypeExpectation != DoctypeExpectation.NO_DOCTYPE_ERRORS) {
                        err("Start tag seen without seeing a doctype first.");
                    }
                    /*
                     * 
                     * Set the document to quirks mode.
                     */
                    documentMode(DocumentMode.QUIRKS_MODE, null, null, false);
                    /*
                     * Then, switch to the root element phase of the tree
                     * construction stage
                     */
                    phase = Phase.ROOT_ELEMENT;
                    /*
                     * and reprocess the current token.
                     */
                    continue;
                case ROOT_ELEMENT:
                    // optimize error check and streaming SAX by hoisting
                    // "html" handling here.
                    if ("html" == name) {
                        if (attributes.getLength() == 0) {
                            // This has the right magic side effect that it
                            // makes attributes in SAX Tree mutable.
                            appendHtmlElementToDocument();
                        } else {
                            appendHtmlElementToDocument(attributes);
                        }
                        phase = Phase.BEFORE_HEAD;
                        return;
                    } else {
                        /*
                         * Create an HTMLElement node with the tag name html, in
                         * the HTML namespace. Append it to the Document object.
                         */
                        appendHtmlElementToDocument();
                        /* Switch to the main phase */
                        phase = Phase.BEFORE_HEAD;
                        /*
                         * reprocess the current token.
                         * 
                         */
                        continue;
                    }
                case BEFORE_HEAD:
                    if ("head" == name) {
                        /*
                         * A start tag whose tag name is "head"
                         * 
                         * Create an element for the token.
                         * 
                         * Set the head element pointer to this new element
                         * node.
                         * 
                         * Append the new element to the current node and push
                         * it onto the stack of open elements.
                         */
                        appendToCurrentNodeAndPushHeadElement(attributes);
                        /*
                         * 
                         * Change the insertion mode to "in head".
                         * 
                         */
                        phase = Phase.IN_HEAD;
                        return;
                    }

                    /*
                     * Any other start tag token
                     */

                    /*
                     * Act as if a start tag token with the tag name "head" and
                     * no attributes had been seen,
                     */
                    appendToCurrentNodeAndPushHeadElement(EmptyAttributes.EMPTY_ATTRIBUTES);
                    phase = Phase.IN_HEAD;
                    /*
                     * then reprocess the current token.
                     * 
                     * This will result in an empty head element being
                     * generated, with the current token being reprocessed in
                     * the "after head" insertion mode.
                     */
                    continue;
                case AFTER_HEAD:
                    if ("body" == name) {
                        if (attributes.getLength() == 0) {
                            // This has the right magic side effect that it
                            // makes attributes in SAX Tree mutable.
                            appendToCurrentNodeAndPushBodyElement();
                        } else {
                            appendToCurrentNodeAndPushBodyElement(attributes);
                        }
                        phase = Phase.IN_BODY;
                        return;
                    } else if ("frameset" == name) {
                        appendToCurrentNodeAndPushElement(name, attributes);
                        phase = Phase.IN_FRAMESET;
                        return;
                    } else if ("base" == name) {
                        err("\u201Cbase\u201D element outside \u201Chead\u201D.");
                        if (nonConformingAndStreaming) {
                            pushHeadPointerOntoStack();
                        }
                        createElementAppendToCurrent(name, attributes);
                        if (nonConformingAndStreaming) {
                            popCurrentNode(); // head
                        }
                        return;
                    } else if ("link" == name) {
                        err("\u201Clink\u201D element outside \u201Chead\u201D.");
                        if (nonConformingAndStreaming) {
                            pushHeadPointerOntoStack();
                        }
                        createElementAppendToCurrent(name, attributes);
                        if (nonConformingAndStreaming) {
                            popCurrentNode(); // head
                        }
                        return;
                    } else if ("meta" == name) {
                        err("\u201Cmeta\u201D element outside \u201Chead\u201D.");
                        // XXX do chaset stuff
                        if (nonConformingAndStreaming) {
                            pushHeadPointerOntoStack();
                        }
                        createElementAppendToCurrent(name, attributes);
                        if (nonConformingAndStreaming) {
                            popCurrentNode(); // head
                        }
                        return;
                    } else if ("script" == name) {
                        err("\u201Cscript\u201D element between \u201Chead\u201D and \u201Cbody\u201D.");
                        if (nonConformingAndStreaming) {
                            pushHeadPointerOntoStack();
                        }
                        appendToCurrentNodeAndPushElement(name, attributes);
                        cdataOrRcdataTimesToPop = nonConformingAndStreaming ? 1
                                : 2; // pops head
                        tokenizer.setContentModelFlag(ContentModelFlag.CDATA,
                                name);
                        return;
                    } else if ("style" == name) {
                        err("\u201Cstyle\u201D element between \u201Chead\u201D and \u201Cbody\u201D.");
                        if (nonConformingAndStreaming) {
                            pushHeadPointerOntoStack();
                        }
                        appendToCurrentNodeAndPushElement(name, attributes);
                        cdataOrRcdataTimesToPop = nonConformingAndStreaming ? 1
                                : 2; // pops head
                        tokenizer.setContentModelFlag(ContentModelFlag.CDATA,
                                name);
                        return;
                    } else if ("title" == name) {
                        err("\u201Ctitle\u201D element outside \u201Chead\u201D.");
                        if (nonConformingAndStreaming) {
                            pushHeadPointerOntoStack();
                        }
                        appendToCurrentNodeAndPushElement(name, attributes);
                        cdataOrRcdataTimesToPop = nonConformingAndStreaming ? 1
                                : 2; // pops head
                        tokenizer.setContentModelFlag(ContentModelFlag.RCDATA,
                                name);
                        return;
                    } else {
                        appendToCurrentNodeAndPushBodyElement();
                        phase = Phase.IN_BODY;
                        continue;
                    }
                case TRAILING_END:
                    err("Stray \u201C" + name + "\u201D start tag.");
                    phase = previousPhaseBeforeTrailingEnd;
                    continue;
            }
        }
    }

    public final void endTag(String name, Attributes attributes)
            throws SAXException {
        needToDropLF = false;
        if (cdataOrRcdataTimesToPop > 0) {
            while (cdataOrRcdataTimesToPop > 0) {
                popCurrentNode();
                cdataOrRcdataTimesToPop--;
            }
            return;
        }

        for (;;) {
            switch (phase) {
                case IN_ROW:
                    if ("tr" == name) {
                        int eltPos = findLastOrRoot("tr");
                        if (eltPos == 0) {
                            assert fragment;
                            err("No table row to close.");
                            return;
                        }
                        clearStackBackTo(eltPos);
                        popCurrentNode();
                        phase = Phase.IN_TABLE_BODY;
                        return;
                    } else if ("table" == name) {
                        int eltPos = findLastOrRoot("tr");
                        if (eltPos == 0) {
                            assert fragment;
                            err("No table row to close.");
                            return;
                        }
                        clearStackBackTo(eltPos);
                        popCurrentNode();
                        phase = Phase.IN_TABLE_BODY;
                        continue;
                    } else if ("tbody" == name || "thead" == name || "tfoot" == name) {
                        if (findLastInTableScope(name) == NOT_FOUND_ON_STACK) {
                            err("Stray end tag \u201C" + name + "\u201D.");                            
                            return;
                        }
                        int eltPos = findLastOrRoot("tr");
                        if (eltPos == 0) {
                            assert fragment;
                            err("No table row to close.");
                            return;
                        }
                        clearStackBackTo(eltPos);
                        popCurrentNode();
                        phase = Phase.IN_TABLE_BODY;
                        continue;
                    } else if ("body" == name || "caption" == name || "col" == name || "colgroup" == name || "html" == name || "td" == name || "th" == name) {
                        err("Stray end tag \u201C" + name + "\u201D.");                            
                        return;
                    } else {
                        // fall through to IN_TABLE
                    }
                case IN_TABLE_BODY:
                    if ("tbody" == name || "tfoot" == name || "thead" == name) {
                        int eltPos = findLastOrRoot(name);
                        if (eltPos == 0) {
                            err("Stray end tag \u201C" + name + "\u201D.");
                            return;
                        }
                        clearStackBackTo(eltPos);
                        popCurrentNode();
                        phase = Phase.IN_TABLE;
                        return;
                    } else if ("table" == name) {
                            int eltPos = findLastInTableScopeOrRootTbodyTheadTfoot();
                            if (eltPos == 0) {
                            assert fragment;
                            err("Stray end tag \u201Ctable\u201D.");
                            return;
                        }
                        clearStackBackTo(eltPos);
                        popCurrentNode();
                        phase = Phase.IN_TABLE;
                        continue;
                    } else if ("body" == name || "caption" == name || "col" == name || "colgroup" == name || "html" == name || "td" == name || "th" == name || "tr" == name) {
                        err("Stray end tag \u201C" + name + "\u201D.");
                        return;
                    } else {
                        // fall through to IN_TABLE
                    }
                case IN_TABLE:
                    if ("table" == name) {
                        int eltPos = findLast("table");
                        if (eltPos == NOT_FOUND_ON_STACK) {
                            assert fragment;
                            err("Stray end tag \u201Ctable\u201D.");
                            return;
                        }
                        generateImpliedEndTags();
                        if (currentPtr != eltPos) {
                            err("There were unclosed elements.");
                        }
                        if (currentPtr >= eltPos) {
                            popCurrentNode();
                        }
                        resetTheInsertionMode();
                        return;
                    } else if ("body" == name || "caption" == name || "col" == name || "colgroup" == name || "html" == name || "tbody" == name || "td" == name || "tfoot" == name || "th" == name || "thead" == name || "tr" == name) {
                        err("Stray end tag \u201C" + name + "\u201D.");
                        return;
                    } else {
                        err("Stray end tag \u201C" + name + "\u201D.");                        
                        // fall through to IN_BODY
                    }
                case IN_CAPTION:
                    if ("caption" == name) {
                        int eltPos = findLastInTableScope("caption");
                        if (eltPos == NOT_FOUND_ON_STACK) {
                            return;
                        }
                        generateImpliedEndTags();
                        if (currentPtr != eltPos) {
                            err("Unclosed elements on stack.");
                        }
                        while (currentPtr >= eltPos) {
                            popCurrentNode();
                        }
                        clearTheListOfActiveFormattingElementsUpToTheLastMarker();
                        phase = Phase.IN_TABLE;
                        return;
                    } else if ("table" == name) {
                        err("\u201Ctable\u201D closed but \u201Ccaption\u201D was still open.");
                        int eltPos = findLastInTableScope("caption");
                        if (eltPos == NOT_FOUND_ON_STACK) {
                            return;
                        }
                        generateImpliedEndTags();
                        if (currentPtr != eltPos) {
                            err("Unclosed elements on stack.");
                        }
                        while (currentPtr >= eltPos) {
                            popCurrentNode();
                        }
                        clearTheListOfActiveFormattingElementsUpToTheLastMarker();
                        phase = Phase.IN_TABLE;
                        continue;
                    } else if ("body" == name || "col" == name || "colgroup" == name || "html" == name || "tbody" == name || "td" == name || "tfoot" == name || "th" == name || "thead" == name || "tr" == name) {
                        err("Stray end tag \u201C" + name + "\u201D.");
                        return;                                                                                                        
                    } else {
                        // fall through to IN_BODY
                    }
                case IN_CELL:
                    if ("td" == name || "th" == name) {
                        int eltPos = findLastInTableScope(name);
                        if (eltPos == NOT_FOUND_ON_STACK) {
                            err("Stray end tag \u201C" + name + "\u201D.");
                            return;                            
                        }
                        generateImpliedEndTags();
                        if (!isCurrent(name)) {
                            err("Unclosed elements.");
                        }
                        while (currentPtr >= eltPos) {
                            popCurrentNode();
                        }
                        clearTheListOfActiveFormattingElementsUpToTheLastMarker();
                        phase = Phase.IN_ROW;
                        return;
                    } else if ("table" == name || "tbody" == name || "tfoot" == name || "thead" == name || "tr" == name) {
                        if (findLastInTableScope(name) == NOT_FOUND_ON_STACK) {
                            err("Stray end tag \u201C" + name + "\u201D.");
                            return;                                                        
                        }
                        closeTheCell(findLastInTableScopeTdTh());
                        continue;
                    } else if ("body" == name || "caption" == name || "col" == name || "colgroup" == name || "html" == name) {
                        err("Stray end tag \u201C" + name + "\u201D.");
                        return;                                                                                
                    } else {
                        // fall through to IN_BODY
                    }
                case IN_BODY:
                    if ("body" == name) {
                        if (!isSecondOnStackBody()) {
                            assert fragment;
                            err("Stray end tag \u201Cbody\u201D.");
                            return;
                        }
                        assert currentPtr >= 1;
                        for (int i = 2; i <= currentPtr; i++) {
                            String stackName = stack[i].name;
                            if (!("dd" == stackName || "dt" == stackName || "li" == stackName
                                    || "p" == stackName)) {
                                err("End tag for \u201Cbody\u201D seen but there were unclosed elements.");
                                break;
                            }
                        }
                        phase = Phase.AFTER_BODY;
                        return;
                    } else if ("html" == name) {
                        if (!isSecondOnStackBody()) {
                            assert fragment;
                            err("Stray end tag \u201Chtml\u201D.");
                            return;
                        }
                        for (int i = 0; i <= currentPtr; i++) {
                            String stackName = stack[i].name;
                            if ("dd" == stackName || "dt" == stackName || "li" == stackName
                                    || "p" == stackName || "tbody" == stackName || "td" == stackName
                                    || "tfoot" == stackName || "th" == stackName || "thead" == stackName || "tr" == stackName || "body" == stackName || "html" == stackName) {
                                err("End tag for \u201Cbody\u201D seen but there were unclosed elements.");
                                break;
                            }
                        }
                        phase = Phase.AFTER_BODY;
                        continue;
                    } else if ("div" == name || "blockquote" == name
                            || "ul" == name || "ol" == name || "pre" == name
                            || "dl" == name || "fieldset" == name
                            || "address" == name || "center" == name
                            || "dir" == name || "listing" == name
                            || "menu" == name) {
                        int eltPos = findLastInScope(name);
                        if (eltPos != NOT_FOUND_ON_STACK) {
                            generateImpliedEndTags();
                        }
                        if (!isCurrent(name)) {
                            err("End tag \u201C" + name + "\u201D seen but there were unclosed elements.");
                        }
                        while (currentPtr >= eltPos) {
                            popCurrentNode();
                        }
                        return;
                    } else if ("form" == name) {
                        int eltPos = findLastInScope(name);
                        if (eltPos != NOT_FOUND_ON_STACK) {
                            generateImpliedEndTags();
                        }
                        if (!isCurrent(name)) {
                            err("End tag \u201Cform\u201D seen but there were unclosed elements.");
                        } else {
                            popCurrentNode();
                        }
                        formPointer = null;
                        return;
                    } else if ("p" == name) {
                        if (!isCurrent(name)) {
                            err("End tag \u201Cp\u201D seen but there were unclosed elements.");
                        }
                        int eltPos = findLastInScope(name);
                        if (eltPos != NOT_FOUND_ON_STACK) {
                            while (currentPtr >= eltPos) {
                                popCurrentNode();
                            }
                        } else {
                            createElementAppendToCurrent(name, EmptyAttributes.EMPTY_ATTRIBUTES);
                        }
                        return;
                    } else if ("dd" == name || "dt" == name || "li" == name) {
                        int eltPos = findLastInScope(name);
                        if (eltPos != NOT_FOUND_ON_STACK) {
                            generateImpliedEndTagsExceptFor(name);
                        }
                        if (!isCurrent(name)) {
                            err("End tag \u201C" + name + "\u201D seen but there were unclosed elements.");
                        }
                        while (currentPtr >= eltPos) {
                            popCurrentNode();
                        }
                        return;
                    } else if ("h1" == name || "h2" == name || "h3" == name
                            || "h4" == name || "h5" == name || "h6" == name) {
                        int eltPos = findLastInScopeHn();
                        if (eltPos != NOT_FOUND_ON_STACK) {
                            generateImpliedEndTags();
                        }
                        if (!isCurrent(name)) {
                            err("End tag \u201C" + name + "\u201D seen but there were unclosed elements.");
                        }
                        while (currentPtr >= eltPos) {
                            popCurrentNode();
                        }
                        return;
                    } else if ("a" == name || "b" == name || "big" == name || "em" == name || "font" == name || "i" == name || "nobr" == name || "s" == name || "small" == name || "strike" == name || "strong" == name || "tt" == name || "u" == name) {
                        adoptionAgencyEndTag(name);
                        return;
                    } else if ("button" == name || "marquee" == name || "object" == name) {
                        int eltPos = findLastInScope(name);
                        if (eltPos != NOT_FOUND_ON_STACK) {
                            generateImpliedEndTags();
                        }
                        if (!isCurrent(name)) {
                            err("End tag \u201C" + name + "\u201D seen but there were unclosed elements.");
                        }
                        while (currentPtr >= eltPos) {
                            popCurrentNode();
                        }
                        clearTheListOfActiveFormattingElementsUpToTheLastMarker();
                        return;
                    } else if ("area" == name || "basefont" == name || "bgsound" == name || "br" == name || "embed" == name || "hr" == name || "iframe" == name || "image" == name || "img" == name || "input" == name || "isindex" == name || "noembed" == name || "noframes" == name || "param" == name || "select" == name || "spacer" == name || "table" == name || "textarea" == name || "wbr" == name || (scriptingEnabled && "noscript" == name)) {
                        err("Stray end tag \u201C" + name + "\u201D.");
                        return;
                    } else {
                        if (isCurrent(name)) {
                            popCurrentNode();
                            return;
                        }
                        for(;;) {
                            generateImpliedEndTags();
                            if (isCurrent(name)) {
                                popCurrentNode();
                                return;
                            }
                            StackNode<T> node = stack[currentPtr];
                            if (!(node.scoping || node.special)) {
                                err("Unclosed element \u201C" + node.name
                                        + "\u201D.");
                                popCurrentNode();
                            } else {
                                return;
                            }
                        }
                    }
                case IN_COLUMN_GROUP:
                    if ("colgroup" == name) {
                        if (currentPtr == 0) {
                            assert fragment;
                            err("Garbage in \u201Ccolgroup\u201D fragment.");
                            return;
                        }
                        popCurrentNode();
                        phase = Phase.IN_TABLE;
                        return;                    
                    } else if ("col" == name) {
                        err("Stray end tag \u201Ccol\u201D.");                        
                        return;
                    } else {
                        if (currentPtr == 0) {
                            assert fragment;
                            err("Garbage in \u201Ccolgroup\u201D fragment.");
                            return;
                        }
                        popCurrentNode();
                        phase = Phase.IN_TABLE;
                        continue;                   
                    }
                case IN_SELECT:
                    if ("option" == name) {
                        if (isCurrent("option")) {
                            popCurrentNode();
                            return;
                        } else {
                            err("Stray end tag \u201Coption\u201D");
                            return;
                        }
                    } else if ("optgroup" == name) {
                        if (isCurrent("option") && "optgroup" == stack[currentPtr - 1].name) {
                            popCurrentNode();
                        }
                        if (isCurrent("optgroup")) {
                            popCurrentNode();
                        } else {
                            err("Stray end tag \u201Coptgroup\u201D");
                            return;                            
                        }
                    } else if ("select" == name) {
                        int eltPos = findLastInTableScope("select");
                        if (eltPos == NOT_FOUND_ON_STACK) {
                            assert fragment;
                            err("Stray end tag \u201Cselect\u201D");
                            return;                                                        
                        }
                        while (currentPtr >= eltPos) {
                            popCurrentNode();
                        }
                        resetTheInsertionMode();
                        return;
                    } else {
                        err("Stray end tag \u201C" + name + "\u201D");
                        return;
                    }
                case AFTER_BODY:
                    if ("html" == name) {
                        if (fragment) {
                            err("Stray end tag \u201Chtml\u201D");
                            return;                            
                        } else {
                            previousPhaseBeforeTrailingEnd = Phase.AFTER_BODY;
                            phase = Phase.TRAILING_END;
                            return;
                        }
                    } else {
                        err("Saw an end tag after \u201Cbody\u201D had been closed.");
                        phase = Phase.IN_BODY;
                        continue;
                    }
                case IN_FRAMESET:
                    if ("frameset" == name) {
                        if (currentPtr == 0) {
                            assert fragment;
                            err("Stray end tag \u201Cframeset\u201D");
                            return;
                        }
                        popCurrentNode();
                        if (!fragment && !isCurrent("frameset")) {
                            phase = Phase.AFTER_FRAMESET;                            
                        }
                        return;
                    } else {
                        err("Stray end tag \u201C" + name + "\u201D");
                        return;                        
                    }
                case AFTER_FRAMESET:
                    if ("html" == name) {
                        previousPhaseBeforeTrailingEnd = Phase.AFTER_FRAMESET;
                        phase = Phase.TRAILING_END;
                        return;
                    } else {
                        err("Stray end tag \u201C" + name + "\u201D");
                        return;                        
                    }
                case INITIAL:
                    /*
                     * Parse error.
                     */
                    if (doctypeExpectation != DoctypeExpectation.NO_DOCTYPE_ERRORS) {
                        err("End tag seen without seeing a doctype first.");
                    }
                    /*
                     * 
                     * Set the document to quirks mode.
                     */
                    documentMode(DocumentMode.QUIRKS_MODE, null, null, false);
                    /*
                     * Then, switch to the root element phase of the tree
                     * construction stage
                     */
                    phase = Phase.ROOT_ELEMENT;
                    /*
                     * and reprocess the current token.
                     */
                    continue;
                case ROOT_ELEMENT:
                    /*
                     * Create an HTMLElement node with the tag name html, in the
                     * HTML namespace. Append it to the Document object.
                     */
                    appendHtmlElementToDocument();
                    /* Switch to the main phase */
                    phase = Phase.BEFORE_HEAD;
                    /*
                     * reprocess the current token.
                     * 
                     */
                    continue;
                case BEFORE_HEAD:
                    if ("head" == name || "body" == name || "html" == name || "p" == name || "br" == name) {
                        appendToCurrentNodeAndPushHeadElement(EmptyAttributes.EMPTY_ATTRIBUTES);
                        phase = Phase.IN_HEAD;
                        continue;
                    } else {
                        err("Stray end tag \u201C" + name + "\u201D.");
                        return;
                    }
                case IN_HEAD:
                    if ("head" == name) {
                        popCurrentNode();
                        phase = Phase.AFTER_HEAD;
                        return;
                    } else if ("body" == name || "html" == name || "p" == name || "br" == name) {
                        popCurrentNode();
                        phase = Phase.AFTER_HEAD;
                        continue;
                    } else {
                        err("Stray end tag \u201C" + name + "\u201D.");
                        return;                        
                    }
                case IN_HEAD_NOSCRIPT:
                    if ("noscript" == name) {
                        popCurrentNode();
                        phase = Phase.IN_HEAD;
                        return;
                    } else if ("p" == name || "br" == name) {
                        err("Stray end tag \u201C" + name + "\u201D.");
                        popCurrentNode();
                        phase = Phase.IN_HEAD;
                        continue;
                    } else {
                        err("Stray end tag \u201C" + name + "\u201D.");
                        return;
                    }
                case AFTER_HEAD:
                    appendToCurrentNodeAndPushBodyElement();
                    phase = Phase.IN_BODY;
                    continue;
                case TRAILING_END:
                    err("Stray \u201C" + name + "\u201D end tag.");
                    phase = previousPhaseBeforeTrailingEnd;
                    continue;
            }
        }
    }

    private int findLastInTableScopeOrRootTbodyTheadTfoot() {
        for (int i = currentPtr; i > 0; i--) {
            if (stack[i].name == "tbody" || stack[i].name == "thead" || stack[i].name == "tfoot") {
                return i;
            }
        }
        return 0;
    }

    private int findLast(String name) {
        for (int i = currentPtr; i > 0; i--) {
            if (stack[i].name == name) {
                return i;
            }
        }
        return NOT_FOUND_ON_STACK;
    }
    
    private int findLastInTableScope(String name) {
        for (int i = currentPtr; i > 0; i--) {
            if (stack[i].name == name) {
                return i;
            } else if (stack[i].name == "table") {
                return NOT_FOUND_ON_STACK;                
            }
        }
        return NOT_FOUND_ON_STACK;
    }

    private int findLastInScope(String name) {
        for (int i = currentPtr; i > 0; i--) {
            if (stack[i].name == name) {
                return i;
            } else if (stack[i].scoping) {
                return NOT_FOUND_ON_STACK;                
            }
        }
        return NOT_FOUND_ON_STACK;
    }

    private int findLastInScopeHn() {
        for (int i = currentPtr; i > 0; i--) {
            String name = stack[i].name;
            if ("h1" == name || "h2" == name || "h3" == name || "h4" == name
                    || "h5" == name || "h6" == name) {
                return i;
            } else if (stack[i].scoping) {
                return NOT_FOUND_ON_STACK;
            }
        }
        return NOT_FOUND_ON_STACK;
    }

    private void generateImpliedEndTagsExceptFor(String name) throws SAXException {
        for (;;) {
            String stackName = stack[currentPtr].name;
            if (name != stackName && ("p" == stackName || "li" == stackName || "dd" == stackName || "dt" == stackName)) {
                popCurrentNode();
            } else {
                return;
            }
        }
    }
    
    private void generateImpliedEndTags() throws SAXException {
        for (;;) {
            String stackName = stack[currentPtr].name;
            if ("p" == stackName || "li" == stackName || "dd" == stackName || "dt" == stackName) {
                popCurrentNode();
            } else {
                return;
            }
        }
    }

    private boolean isSecondOnStackBody() {
        return currentPtr >= 1 && stack[1].name == "body";
    }

    private void documentMode(DocumentMode mode, String publicIdentifier,
            String systemIdentifier, boolean html4SpecificAdditionalErrorChecks) {
        if (documentModeHandler != null) {
            documentModeHandler.documentMode(mode, publicIdentifier,
                    systemIdentifier, html4SpecificAdditionalErrorChecks);
        }
    }

    protected void appendDoctypeToDocument(String name,
            String publicIdentifier, String systemIdentifier) {
    }

    private boolean isAlmostStandards(String publicIdentifierLC,
            String systemIdentifierLC) {
        if ("-//w3c//dtd xhtml 1.0 transitional//en".equals(publicIdentifierLC)) {
            return true;
        }
        if ("-//w3c//dtd xhtml 1.0 frameset//en".equals(publicIdentifierLC)) {
            return true;
        }
        if (systemIdentifierLC != null) {
            if ("-//w3c//dtd html 4.01 transitional//en".equals(publicIdentifierLC)) {
                return true;
            }
            if ("-//w3c//dtd html 4.01 frameset//en".equals(publicIdentifierLC)) {
                return true;
            }
        }
        return false;
    }

    private boolean isQuirky(String name, String publicIdentifierLC,
            String systemIdentifierLC, boolean correct) {
        if (!correct) {
            return true;
        }
        if (!"HTML".equalsIgnoreCase(name)) {
            return true;
        }
        if (publicIdentifierLC != null
                && (Arrays.binarySearch(QUIRKY_PUBLIC_IDS, publicIdentifierLC) > -1)) {
            return true;
        }
        if (systemIdentifierLC == null) {
            if ("-//w3c//dtd html 4.01 transitional//en".equals(publicIdentifierLC)) {
                return true;
            } else if ("-//w3c//dtd html 4.01 frameset//en".equals(publicIdentifierLC)) {
                return true;
            }
        } else if ("http://www.ibm.com/data/dtd/v11/ibmxhtml1-transitional.dtd".equals(systemIdentifierLC)) {
            return true;
        }
        return false;
    }

    private String toAsciiLowerCase(String str) {
        if (str == null) {
            return null;
        }
        char[] buf = new char[str.length()];
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (c >= 'A' && c <= 'Z') {
                c += 0x20;
            }
            buf[i] = c;
        }
        return new String(buf);
    }

    private void closeTheCell(int eltPos) throws SAXException {
        generateImpliedEndTags();
        if (eltPos != currentPtr) {
            err("Unclosed elements.");
        }
        while (currentPtr >= eltPos) {
            popCurrentNode();
        }
        clearTheListOfActiveFormattingElementsUpToTheLastMarker();
        phase = Phase.IN_ROW;
        return;
    }

    private int findLastInTableScopeTdTh() {
        for (int i = currentPtr; i > 0; i--) {
            String name = stack[i].name;
            if ("td" == name || "th" == name) {
                return i;
            } else if (name == "table") {
                return NOT_FOUND_ON_STACK;                
            }
        }
        return NOT_FOUND_ON_STACK;
    }

    private void clearStackBackTo(int eltPos) throws SAXException {
        if (eltPos != currentPtr) {
            err("Unclosed elements.");
            while(currentPtr > eltPos) { // > not >= intentional
                popCurrentNode();
            }
        }
    }

    private void resetTheInsertionMode() {
        String name;
        for (int i = currentPtr; i >= 0; i--) {
            name = stack[i].name;
            if (i == 0) {
                if (!(context.name == "td" || context.name == "th")) {
                    name = context.name;
                }
            }
            if ("select" == name) {
                phase = Phase.IN_SELECT;
                return;
            } else if ("td" == name || "th" == name) {
                phase = Phase.IN_CELL;
                return;
            } else if ("tr" == name) {
                phase = Phase.IN_ROW;
                return;
            } else if ("tbody" == name || "thead" == name || "tfoot" == name) {
                phase = Phase.IN_TABLE_BODY;
                return;
            } else if ("caption" == name) {
                phase = Phase.IN_CAPTION;
                return;
            } else if ("colgroup" == name) {
                phase = Phase.IN_COLUMN_GROUP;
                return;
            } else if ("table" == name) {
                phase = Phase.IN_TABLE;
                return;
            } else if ("head" == name) {
                phase = Phase.IN_BODY; // really
                return;
            } else if ("body" == name) {
                phase = Phase.IN_BODY;
                return;
            } else if ("frameset" == name) {
                phase = Phase.IN_FRAMESET;
                return;
            } else if ("html" == name) {
                if (headPointer == null) {
                    phase = Phase.BEFORE_HEAD;                    
                } else {
                    phase = Phase.AFTER_HEAD;
                }
                return;
            } else if (i == 0) {
                phase = Phase.IN_BODY;
                return;
            } 
        }
    }

    /**
     * @throws SAXException 
     * 
     */
    private void implicitlyCloseP() throws SAXException {
        int eltPos = findLastInScope("p");
        if (eltPos == NOT_FOUND_ON_STACK) {
            return;
        }
        if (currentPtr != eltPos) {
            err("Unclosed elements.");
        }
        while (currentPtr >= eltPos) {
            popCurrentNode();
        }
    }

    private void push(StackNode<T> node) {
        currentPtr++;
        if (currentPtr == stack.length) {
            StackNode<T>[] newStack = new StackNode[stack.length + 64];
            System.arraycopy(stack, 0, newStack, 0, stack.length);
            stack = newStack;
        }
        stack[currentPtr] = node;
    }

    private void append(StackNode<T> node) {
        listPtr++;
        if (listPtr == listOfActiveFormattingElements.length) {
            StackNode<T>[] newList = new StackNode[listOfActiveFormattingElements.length + 64];
            System.arraycopy(listOfActiveFormattingElements, 0, newList, 0, listOfActiveFormattingElements.length);
            listOfActiveFormattingElements = newList;
        }
        listOfActiveFormattingElements[listPtr] = node;
    }
    
    private void insertMarker() {
        append(MARKER);
    }

    private void clearTheListOfActiveFormattingElementsUpToTheLastMarker() {
        for (;;) {
            if (listOfActiveFormattingElements[listPtr--] == MARKER) {
                return;
            }
        }
    }

    private boolean isCurrent(String name) {
        return name == stack[currentPtr].name;
    }

    private void appendToCurrentNodeAndPushFormattingElement(String name,
            Attributes attributes) throws SAXException {
        T elt = createElementAppendToCurrentAndPush(name, attributes);
        StackNode<T> node = new StackNode<T>(name, elt);
        push(node);
        append(node);
    }

    private void removeFromStack(int pos) throws SAXException {
        if (currentPtr == pos) {
            popCurrentNode();
        } else {
            if (conformingAndStreaming) {
                fatal();
            } else if (nonConformingAndStreaming) {
                while (currentPtr >= pos) {
                    popCurrentNode();
                }
            } else {
                System.arraycopy(stack, pos + 1, stack, pos, currentPtr - pos);
                currentPtr--;
            }
        }
    }
    
    private void removeFromStack(StackNode<T> node) throws SAXException {
        if (stack[currentPtr] == node) {
            popCurrentNode();
        } else {
            int pos = currentPtr - 1;
            while (pos >= 0 && stack[pos] != node) {
                pos--;
            }
            if (pos == -1) {
                // dead code?
                return;
            }
            if (conformingAndStreaming) {
                fatal();
            } else if (nonConformingAndStreaming) {
                while (currentPtr >= pos) {
                    popCurrentNode();
                }
            } else {
                System.arraycopy(stack, pos + 1, stack, pos, currentPtr - pos);
                currentPtr--;
            }
        }
    }

    private void removeFromListOfActiveFormattingElements(int pos) {
        if (pos == listPtr) {
            listPtr--;
            return;
        }
        System.arraycopy(listOfActiveFormattingElements, pos + 1, listOfActiveFormattingElements, pos, listPtr - pos);
        listPtr--;
    }

    private void adoptionAgencyEndTag(String name) throws SAXException {
        for (;;) {
            int formattingEltListPos = listPtr;
            while (formattingEltListPos > -1) {
                String listName = listOfActiveFormattingElements[formattingEltListPos].name;
                if (listName == name) {
                    break;
                } else if (listName == null) {
                    formattingEltListPos = -1;
                    break;
                }
                formattingEltListPos--;
            }
            if (formattingEltListPos == -1) {
                err("No element \u201C" + name + "\u201D to close.");
                return;
            }
            StackNode<T> formattingElt = listOfActiveFormattingElements[formattingEltListPos];
            int formattingEltStackPos = currentPtr;
            boolean inScope = true;
            while (formattingEltStackPos > -1) {
                StackNode<T> node = stack[formattingEltStackPos];
                if (node == formattingElt) {
                    break;
                } else if (node.scoping) {
                    inScope = false;
                }
                formattingEltStackPos--;
            }
            if (formattingEltStackPos == -1) {
                err("No element \u201C" + name + "\u201D to close.");
                removeFromListOfActiveFormattingElements(formattingEltListPos);
                return;
            }
            if (!inScope) {
                err("No element \u201C" + name + "\u201D to close.");
                return;
            }
            // stackPos now points to the formatting element and it is in scope
            if (formattingEltStackPos != currentPtr) {
                err("End tag \u201C" + name + "\u201D violates nesting rules.");
            }
            int furthestBlockPos = formattingEltStackPos + 1;
            while (furthestBlockPos <= currentPtr) {
                StackNode<T> node = stack[furthestBlockPos];
                if (node.scoping || node.special) {
                    break;
                }
                furthestBlockPos++;
            }
            if (furthestBlockPos > currentPtr) {
                // no furthest block
                while (currentPtr >= formattingEltStackPos) {
                    popCurrentNode();
                }
                removeFromListOfActiveFormattingElements(formattingEltListPos);
                return;
            }
            StackNode<T> commonAncestor = stack[formattingEltStackPos - 1];
            StackNode<T> furthestBlock = stack[furthestBlockPos];
            detachFromParent(furthestBlock.node);
            int bookmark = formattingEltListPos;
            int nodePos = furthestBlockPos;
            StackNode<T> lastNode = furthestBlock;
            for(;;) {
                nodePos--;
                StackNode<T> node = stack[nodePos];
                int nodeListPos = findInListOfActiveFormattingElements(node);
                if (nodeListPos == -1) {
                    assert formattingEltStackPos < nodePos;
                    assert bookmark < nodePos;
                    assert furthestBlockPos > nodePos;
                    removeFromStack(nodePos);
                    furthestBlockPos--;
                    continue;
                }
                if (nodePos == formattingEltStackPos) {
                    break;
                }
                if (nodePos == furthestBlockPos) {
                    bookmark = nodeListPos + 1;
                }
                if (hasChildren(node.node)) {
                    assert node == listOfActiveFormattingElements[nodeListPos];
                    assert node == stack[nodePos];
                    T clone = shallowClone(node.node);
                    node = new StackNode<T>(node.name, clone, node.scoping, node.special, node.fosterParenting);
                    listOfActiveFormattingElements[nodeListPos] = node;
                    stack[nodePos] = node;                    
                }
                detachFromParentAndAppendToNewParent(lastNode.node, node.node);
                lastNode = node;
            }
            detachFromParentAndAppendToNewParent(lastNode.node, commonAncestor.node);
            T clone = shallowClone(formattingElt.node);
            StackNode<T> formattingClone = new StackNode<T>(formattingElt.name, clone, formattingElt.scoping, formattingElt.special, formattingElt.fosterParenting);
            detachFromParentAndAppendToNewParent(clone, furthestBlock.node);
            removeFromListOfActiveFormattingElements(formattingEltListPos);
            insertIntoListOfActiveFormattingElements(formattingClone, bookmark);
            assert formattingEltStackPos < furthestBlockPos;
            removeFromStack(formattingEltStackPos);
            insertIntoStack(formattingClone, furthestBlockPos + 1);
        }
    }

    private void insertIntoStack(StackNode<T> formattingClone, int position) {
        assert currentPtr + 1 < stack.length;
        if (position <= currentPtr) {
            System.arraycopy(listOfActiveFormattingElements, position, listOfActiveFormattingElements, position + 1, (currentPtr - position) + 1);
        }
        currentPtr++;
        listOfActiveFormattingElements[position] = formattingClone;        
    }

    private void insertIntoListOfActiveFormattingElements(StackNode<T> formattingClone, int bookmark) {
        assert listPtr + 1 < listOfActiveFormattingElements.length;
        if (bookmark <= listPtr) {
            System.arraycopy(listOfActiveFormattingElements, bookmark, listOfActiveFormattingElements, bookmark + 1, (listPtr - bookmark) + 1);
        }
        listPtr++;
        listOfActiveFormattingElements[bookmark] = formattingClone;
    }

    private int findInListOfActiveFormattingElements(StackNode<T> node) {
        for (int i = listPtr; i >= 0; i--) {
            if (node == listOfActiveFormattingElements[i]) {
                return i;
            }
        }
        return -1;
    }

    private int findInListOfActiveFormattingElementsContainsBetweenEndAndLastMarker(
            String name) {
        for (int i = listPtr; i >= 0; i--) {
            StackNode<T> node = listOfActiveFormattingElements[i];
            if (node.name == name) {
                return i;
            } else if (node == MARKER) {
                return -1;
            }
        }        
        return -1;
    }

    private int findDdOrDtToPop() {
        for (int i = currentPtr; i >= 0; i--) {
            StackNode<T> node = stack[i];
            if ("dd" == node.name || "dt" == node.name) {
                return i;
            } else if ((node.scoping || node.special) && !("div" == node.name || "address" == node.name)) {
                return NOT_FOUND_ON_STACK;
            }
        }
        return NOT_FOUND_ON_STACK;
    }

    private int findLiToPop() {
        for (int i = currentPtr; i >= 0; i--) {
            StackNode<T> node = stack[i];
            if ("li" == node.name) {
                return i;
            } else if ((node.scoping || node.special) && !("div" == node.name || "address" == node.name)) {
                return NOT_FOUND_ON_STACK;
            }
        }
        return NOT_FOUND_ON_STACK;
    }

    private void appendToCurrentNodeAndPushFormElement(Attributes attributes) throws SAXException {
        T elt = createElementAppendToCurrentAndPush("form", attributes);
        formPointer = elt;
        StackNode<T> node = new StackNode<T>("form", elt);
        push(node);
    }

    private void addAttributesToBody(Attributes attributes) throws SAXException {
        if (currentPtr >= 1) {
            StackNode<T> body = stack[1];
            if (body.name == "body") {
                addAttributesToElement(body.node, attributes);                
            }
        }
    }

    private void pushHeadPointerOntoStack() {
        push(new StackNode<T>("head", headPointer));
    }

    private void appendHtmlElementToDocument(Attributes attributes) throws SAXException {
        T elt = createHtmlElementSetAsRootAndPush(attributes);
        StackNode<T> node = new StackNode<T>("html", elt);
        push(node);
    }

    private void appendCharToFosterParent(char c) {
        // TODO Auto-generated method stub

    }

    /**
     * @throws SAXException 
     * 
     */
    private void reconstructTheActiveFormattingElements() throws SAXException {
        if (listPtr == -1) {
            return;
        }
        if (listOfActiveFormattingElements[listPtr] == MARKER) {
            return;
        }
        int entryPos = listPtr;
        for(;;) {
            entryPos--;
            if (entryPos == -1) {
                break;
            }
            if (listOfActiveFormattingElements[entryPos] == MARKER) {
                break;
            }
            if (isInStack(listOfActiveFormattingElements[entryPos])) {
                break;
            }
        }
        while (entryPos < listPtr) {
            entryPos++;
            StackNode<T> entry = listOfActiveFormattingElements[entryPos];
            T clone = shallowClone(entry.node);
            StackNode<T> entryClone = new StackNode<T>(entry.name, clone, entry.scoping, entry.special, entry.fosterParenting);
            StackNode<T> currentNode = stack[currentPtr];
            if (currentNode.fosterParenting) {
                insertIntoFosterParent(clone);                
            } else {
                detachFromParentAndAppendToNewParent(clone, currentNode.node);
            }
            push(entryClone);
            listOfActiveFormattingElements[entryPos] = entryClone;
        }
    }

    private void insertIntoFosterParent(T child) throws SAXException {
        int eltPos = findLastOrRoot("table");
        T elt = stack[eltPos].node;
        if (eltPos == 0) {
            detachFromParentAndAppendToNewParent(child, elt);
            return;
        }
        T parent = parentElementFor(elt);
        if (parent == null) {
            detachFromParentAndAppendToNewParent(child, stack[eltPos - 1].node);            
        } else {
            insertBefore(child, elt, parent);            
        }
    }

    private boolean isInStack(StackNode<T> node) {
        for (int i = currentPtr; i >= 0; i--) {
            if (stack[i] == node) {
                return true;
            }
        }
        return false;
    }

    private void popCurrentNode() throws SAXException {
        StackNode<T> node = stack[currentPtr];
        currentPtr--;
        elementPopped(node.name, stack[currentPtr].node);
    }

    private void appendToCurrentNodeAndPushHeadElement(
            Attributes attributes) throws SAXException {
        T elt = createElementAppendToCurrentAndPush("head", attributes);
        headPointer = elt;
        StackNode<T> node = new StackNode<T>("head", elt);
        push(node);
    }

    private void appendToCurrentNodeAndPushBodyElement(
            Attributes attributes) throws SAXException {
        appendToCurrentNodeAndPushElement("body", attributes);
    }

    private void appendToCurrentNodeAndPushBodyElement() throws SAXException {
        appendToCurrentNodeAndPushBodyElement(tokenizer.newAttributes());
    }

    private void appendToCurrentNodeAndPushElement(String name,
            Attributes attributes) throws SAXException {
        T elt = createElementAppendToCurrentAndPush(name, attributes);
        StackNode<T> node = new StackNode<T>(name, elt);
        push(node);        
    }

    private void appendHtmlElementToDocument() throws SAXException {
        appendHtmlElementToDocument(tokenizer.newAttributes());
    }

    private void appendToCurrentNodeAndPushElementWithFormPointer(String name, Attributes attributes) throws SAXException {
        T elt = createElementAppendToCurrentAndPush(name, attributes, formPointer);
        StackNode<T> node = new StackNode<T>(name, elt);
        push(node);
    }

    private int findLastOrRoot(String name) {
        for (int i = currentPtr; i > 0; i--) {
            if (stack[i].name == name) {
                return i;
            }
        }
        return 0;
    }
    
    protected T createElementAppendToCurrentAndPush(String name,
            Attributes attributes, T form) throws SAXException {
        return createElementAppendToCurrentAndPush(name, attributes);
    }

    protected void createElementAppendToCurrent(String name,
            Attributes attributes, T form) throws SAXException {
        createElementAppendToCurrentAndPush(name, attributes, form);
        elementPopped(name, stack[currentPtr].node);
    }
    
    protected void createElementAppendToCurrent(String name, Attributes attributes) throws SAXException {
        createElementAppendToCurrentAndPush(name, attributes);
        elementPopped(name, stack[currentPtr].node);    
    }

    protected abstract T createElementAppendToCurrentAndPush(String name,
            Attributes attributes) throws SAXException;
    
    protected abstract void elementPopped(String poppedElemenName, T newCurrentNode) throws SAXException;

    protected abstract T createHtmlElementSetAsRootAndPush(Attributes attributes) throws SAXException;
    
    protected abstract void detachFromParent(T element) throws SAXException;

    protected abstract boolean hasChildren(T element) throws SAXException;
    
    protected abstract T shallowClone(T element) throws SAXException;
    
    protected abstract void detachFromParentAndAppendToNewParent(T child, T newParent) throws SAXException;

    /**
     * Get the parent element. MUST return <code>null</code> if there is no parent
     * <em>or</em> the parent is not an element.
     */
    protected abstract T parentElementFor(T child) throws SAXException;
    
    protected abstract void insertBefore(T child, T sibling, T parent) throws SAXException;
    
    protected abstract void appendCharactersToCurrentNode(char[] buf,
            int start, int length) throws SAXException;
    
    protected abstract void appendCommentToCurrentNode(char[] buf, int length) throws SAXException;

    protected abstract void appendCommentToDocument(char[] buf, int length) throws SAXException;

    protected abstract void appendCommentToRootElement(char[] buf, int length) throws SAXException;
    
    protected abstract void addAttributesToElement(T element, Attributes attributes) throws SAXException;

    protected void start() throws SAXException {
        
    }

    protected void end() throws SAXException {
        
    }
    
    /**
     * @see nu.validator.htmlparser.TokenHandler#wantsComments()
     */
    public abstract boolean wantsComments() throws SAXException;

    /**
     * Sets the errorHandler.
     * 
     * @param errorHandler the errorHandler to set
     */
    public void setErrorHandler(ErrorHandler errorHandler) {
        this.errorHandler = errorHandler;
    }

}