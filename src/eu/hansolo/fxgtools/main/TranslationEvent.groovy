package eu.hansolo.fxgtools.main

/**
 * Author: hansolo
 * Date  : 07.09.11
 * Time  : 12:31
 */
class TranslationEvent extends EventObject{
    private static final long serialVersionUID       = 1L;
    private final             TranslationState STATE

  public TranslationEvent(final Object SOURCE, final TranslationState STATE)
  {
    super(SOURCE)
    this.STATE = STATE
  }

  public TranslationState getState()
  {
    return STATE
  }
}
